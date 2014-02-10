package com.degoo.nat.tests.comparison;

import com.degoo.nat.tests.guice.integration.IntegrationTestInjectFactory;
import com.degoo.tests.DataLoader;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 *
 */
public class PlainSocketNode {
	private static final Logger log = LoggerFactory.getLogger(PlainSocketNode.class);
	private static Injector injector;
	private final int numSend;
	private final DataLoader dataLoader;
	private final String testFileName;
	private final String testFileDir;

	@Inject
	public PlainSocketNode(@Named("numSend") int numSend,
						   @Named("testFilesDir") String testFileDir,
						   @Named("testFileName") String testFileName,
						   DataLoader dataLoader) throws IOException {
		this.numSend = numSend;
		this.dataLoader = dataLoader;
		this.testFileName = testFileName;
		this.testFileDir = testFileDir;
	}

	public static void main(String[] args) throws Exception {
		//The arg will be: sequenceNumber, username, password, another node.
		validateArgs(args);

		injector = IntegrationTestInjectFactory.createNATInjector();
		PlainSocketNode node = injector.getInstance(PlainSocketNode.class);

		//First as sender, second as receiver
		if(Integer.valueOf(args[0]) == 0){
			Sender sender = new Sender(node.numSend, node.dataLoader, node.testFileName, node.testFileDir);
			Thread.sleep(1000);
			sender.sendData();
		} else{
			Receiver receiver = new Receiver(node.dataLoader, node.testFileName, node.testFileDir);
			receiver.listen();
		}
	}

	private static void validateArgs(String[] args) {
		if(args.length <= 2){
			log.error("Wrong arguments numbers. Process quit!");
			System.exit(0);
		}
	}

	private static class Sender {
		private final static Logger log = LoggerFactory.getLogger(Sender.class);
		private final int numSend;
		private final DataLoader dataLoader;
		private final Socket senderSocket;
		private final byte[] testData;

		public Sender(int numSend,
					  DataLoader dataLoader,
					  String testFileName,
					  String testFileDir) throws IOException {
			this.numSend = numSend;
			this.dataLoader = dataLoader;
			this.testData = dataLoader.readData(testFileName, testFileDir);
			this.senderSocket = new Socket("localhost", 9998);
			init();
			
		}

		private void init() throws SocketException {
			senderSocket.setSoTimeout(0);
		}

		public void sendData() throws IOException, InterruptedException {
			final OutputStream os = senderSocket.getOutputStream();
			int delay = 0;
			log.info("Start sending {} data out...", numSend);

			for(int i=0; i< numSend; i++) {
				try {
					os.write(testData);
					//log.info("Data send at: {}", System.currentTimeMillis());
				} catch (IOException e) {
					log.error("Error on send data: {}", e.getMessage());
				}
			}

			os.flush();
			senderSocket.close();
			Thread.sleep(5000);
		}
	}

	private static class Receiver{
		private final static Logger log = LoggerFactory.getLogger(Receiver.class);
		private final DataLoader dataLoader;
		private final String testFileName;
		private final String testFileDir;
		private final ServerSocket receiverSocket;

		public Receiver(DataLoader dataLoader,
						String testFileName,
						String testFileDir) throws IOException {
			this.dataLoader = dataLoader;
			this.testFileName = testFileName;
			this.testFileDir = testFileDir;
			receiverSocket = new ServerSocket(9998);
			receiverSocket.setSoTimeout(0);
		}

		public void listen() throws Exception {
			byte[] readBuf = new byte[(int) dataLoader.getFileSize(testFileName, testFileDir)];

			Socket socket = receiverSocket.accept();
			InputStream is = socket.getInputStream();

			while(is.read(readBuf) != -1)  {}

			log.info("Data receive done.");

			socket.close();
			receiverSocket.close();
		}
	}
}
