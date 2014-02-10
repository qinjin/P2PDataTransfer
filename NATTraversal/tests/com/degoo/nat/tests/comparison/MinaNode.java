package com.degoo.nat.tests.comparison;

import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NetworkErrorCode;
import com.degoo.nat.tests.guice.integration.IntegrationTestInjectFactory;
import com.degoo.tests.DataLoader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 */
public class MinaNode implements IoHandler {
	private static final Logger log = LoggerFactory.getLogger(MinaNode.class);
	private static Injector injector;

	private final int numSend;
	private final String testFileDir;
	private final String testFileName;
	private final byte[] testData;

	private  boolean isStoppedReceiving;
	private  NioSocketAcceptor acceptor;

	@Inject
	public MinaNode(@Named("numSend") int numSend,
					@Named("testFilesDir") String testFileDir,
					@Named("testFileName") String testFileName,
					DataLoader dataLoader) throws IOException {
		this.numSend = numSend;
		this.testFileDir = testFileDir;
		this.testFileName = testFileName;
		this.testData =  dataLoader.readData(testFileName, testFileDir);
	}

	//Server method
	public void initServer(int localPort) throws IOException {
		acceptor = new NioSocketAcceptor();
		addFilters(acceptor);
		acceptor.setReuseAddress(true);
		acceptor.setHandler(this);
		InetSocketAddress localNodeAddress = new InetSocketAddress("localhost", localPort);
		acceptor.bind(localNodeAddress);
		log.info("Server side node binding to:" + localNodeAddress);
	}

	//Server method
	public void unbind() {
		if(acceptor != null) {
			acceptor.unbind();
		}
	}

	//Client method
	public void send(int serverPort) throws IOException {
		IoSession session = connect(serverPort);
		log.info("Start sending {} data out...", numSend);
		for(int i =0; i<numSend; i++){
			//log.info("Send data {}", i);
			sendData(session, NATMessage.newBuilder().setErrorCode(NetworkErrorCode.NoError).setData(ByteString.copyFrom(testData)).build());
		}
		session.write("FIN");
		session.close(false);
	}

	//Client method
	private IoSession connect(int serverPort) {
		InetSocketAddress remoteNodeAddress = new InetSocketAddress("localhost", serverPort);
		return connectToAddress(remoteNodeAddress);
	}

	//Client method
	private void sendData(IoSession session, NATMessage natMessage) throws IOException {
		if(session == null) throw new IOException("Null Session");
		session.write(natMessage);
	}

	//Client method
	private IoSession connectToAddress(InetSocketAddress address) {

		try {
			final NioSocketConnector connector = new NioSocketConnector();
			addFilters(connector);
			connector.setHandler(this);
			connector.setConnectTimeoutMillis(1000);
			connector.getSessionConfig().setWriteTimeout(240);
			log.info("Start to connect to server: {}", address);
			final ConnectFuture future = connector.connect(address);
			future.awaitUninterruptibly();
			if (!future.isConnected()) {
				log.error("Connect to server timeout.");
				return null;
			}
			IoSession session = future.getSession();
			return session;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	//Common method
	private static void addFilters(IoService service) {
		final ObjectSerializationCodecFactory objectProtocolFactory = new ObjectSerializationCodecFactory();
		objectProtocolFactory.setDecoderMaxObjectSize(1024 * 1024 * 100);
		final ProtocolCodecFilter protocolCodecFilter = new ProtocolCodecFilter(objectProtocolFactory);
		service.getFilterChain().addLast("log", new LoggingFilter("log"));
		service.getFilterChain().addLast("codec", protocolCodecFilter);
	}

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		log.debug("Session is created.");
	}

	@Override
	public void sessionOpened(IoSession session) throws Exception {
		log.debug("Session is opened.");
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		log.debug("Session is closed.");
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		log.debug("Session is idle (for UDP session).");
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		log.error("Session exception caught: {}", cause.getMessage());
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		if(message instanceof String && String.valueOf(message).equals("FIN")){
			log.info("Data receive done.");
			session.close(false);
			unbind();
			log.debug("Close session and unbind.");
		} else if(message instanceof NATMessage){
			//log.info("NATMessage received.");
		}
	}

	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		//log.info("Data sent", session.getId());
	}


	public static void main(String[] args) {
		//The arg will be: sequenceNumber, local id, localport, another node id.
		validateArgs(args);
		injector = IntegrationTestInjectFactory.createNATInjector();
		MinaNode node = injector.getInstance(MinaNode.class);

		try{
			if(Integer.valueOf(args[0]) == 0) {
				//Sender as client.
				//Waiting for receiver is up.
				Thread.sleep(100);
				node.send(8558);
			} else{
				//Receiver as server.
				log.info("Server started.");
				node.initServer(8558);
			}
		} catch(IOException ex){
			log.error("Mina node exception: " + ex.getMessage());
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			log.error("Mina node exception: " + ex.getMessage());
		}
	}

	private static void validateArgs(String[] args) {
		if(args.length <= 2){
			log.error("Wrong arguments numbers. Process quit!");
			System.exit(0);
		}
	}
//	private static class ServerTaskRunner extends Thread{
//		private Injector injector;
//		private MinaNode minaNode;
//		private int port;
//
//		public ServerTaskRunner(int port) {
//			injector = IntegrationTestInjectFactory.createNATInjector();
//			minaNode = injector.getInstance(MinaNode.class);
//			this.port = port;
//		}
//
//		public MinaNode getMinaNode() {
//			return minaNode;
//		}
//
//		@Override
//		public void run(){
//			try {
//				minaNode.initServer(port);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			log.info("Sever is initialized.");
//		}
//	}
}
