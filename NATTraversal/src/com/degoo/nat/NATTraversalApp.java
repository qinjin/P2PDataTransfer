package com.degoo.nat;

import com.degoo.nat.adapter.NATAdapter;
import com.degoo.nat.adapter.NATAdapterListener;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NetworkErrorCode;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

/**
 *
 */
//TODO: Add a connect and disconnect feature for upper layer to control the creation and
//elimination of a tunnel in the future.
public class NATTraversalApp implements NATAdapterListener{
	private final static Logger log = LoggerFactory.getLogger(NATTraversalApp.class);
	private final NATMessageCracker natMessageCracker;
	private final NATAdapter natAdapter;
	private final String xmppServer;
	private final int xmppServerPort;
	private final int loginTimeOut;
	private final boolean debug;
	private final NATInfoBean stunInfo;
	private final NATInfoBean turnInfo;

	@Inject
	public NATTraversalApp(@Named("server") String xmppServer,
						   @Named("serverPort") int xmppServerPort,
						   @Named("stunAddress") String stunAddr,
						   @Named("stunPort") int stunPort,
						   @Named("turnAddress") String turnAddr,
						   @Named("turnPort") int turnPort,
						   @Named("loginTimeOut")  int loginTimeOut,
						   @Named("debug") boolean debug,
						   NATAdapter natAdapter,
						   NATMessageCracker natMessageCracker) {
		this.natAdapter = natAdapter;
		this.xmppServer = xmppServer;
		this.xmppServerPort = xmppServerPort;
		this.loginTimeOut = loginTimeOut;
		this.stunInfo = new NATInfoBean(stunAddr, stunPort);
		this.turnInfo = new NATInfoBean(turnAddr, turnPort);
		this.natMessageCracker = natMessageCracker;
		this.debug = debug;
	}

	public final void login(final NodeID nodeID, final String password){
		log.debug("Started login to {}:{}...", xmppServer, xmppServerPort);
		natAdapter.login(nodeID, password, xmppServer, xmppServerPort, loginTimeOut);
	}

	public final void logout(){
		natAdapter.logout();
		log.debug("Logged out.");
	}

	public final void send(final NATMessage message) {
		natAdapter.send(message);
	}

	@Override
	public void onLoggedIn(NodeStatusMessage message) {
		setDebug(debug);

		if(initNATInfo()){
			natMessageCracker.onLoggedIn(message);
		} else{
			natMessageCracker.onLoginFailed(NodeStatusMessage.newBuilder().setNodeId(message.getNodeId()).setErrorCode(NetworkErrorCode.InvalidNATInfo).build());
		}
	}

	@Override
	public void onLoggedOut(NodeStatusMessage message) {
		 natMessageCracker.onLoggedOut(message);
	}

	@Override
	public void onLoginFailed(NodeStatusMessage message) {
		natMessageCracker.onLoginFailed(message);
	}

	@Override
	public void onDataReceived(NATMessage message) {
		log.debug("Received data from {}", message.getSenderId().getId());
		natMessageCracker.onReceived(message);
	}

	@Override
	public void onDataReceiveFailed(NATMessage message) {
		natMessageCracker.onDataReceiveFailed(message);
	}

	@Override
	public void onDataSent(NATMessage message) {
		log.debug("Send data to {}", message.getReceiverId().getId());
		natMessageCracker.onSent(message);
	}

	@Override
	public void onDataSendFailed(NATMessage message, int numFailed) {
		natMessageCracker.onDataSendFailed(message, numFailed);
	}

	private void setDebug(boolean isDebug) {
		natAdapter.setDebugMode(isDebug);
	}

	private boolean initNATInfo(){
		Object[] parameters = {stunInfo.getAddr(), stunInfo.getPort(), turnInfo.getAddr(), turnInfo.getPort()};
		log.info("STUN server set to {}:{}, TURN server set to: {}:{}", parameters);
		return natAdapter.setNATInfo(stunInfo.getAddr(), stunInfo.getPort(), turnInfo.getAddr(), turnInfo.getPort());
	}

	private static class NATInfoBean {
		private final String addr;
		private final int port;

		public NATInfoBean(String addr, int port){
			this.addr = addr;
			this.port = port;
		}

		public String getAddr() { return addr;}
	    public int getPort() { return port; }
	}
}
