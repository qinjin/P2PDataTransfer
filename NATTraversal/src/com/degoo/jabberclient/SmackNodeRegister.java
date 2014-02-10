package com.degoo.jabberclient;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SmackNodeRegister implements ConnectionListener, PacketListener, NodeRegister{
	private final static Logger log = LoggerFactory.getLogger(SmackNodeRegister.class);
	private final String server;
	private final int port;
	private final Connection connection;
	private final NodeRegistrationListener nodeRegistrationListener;

	@Inject
	public SmackNodeRegister(@Named("server") String server,
							 @Named("serverPort") int port,
							 NodeRegistrationListener nodeRegistrationListener){
		this.server = server;
		this.port = port;
		this.connection = new XMPPConnection(new ConnectionConfiguration(server, port));
		this.nodeRegistrationListener = nodeRegistrationListener;
	}

	@Override
	public boolean connect() {
		log.info("Started connect to Jabber server...");
		try {
			connection.connect();
		} catch (XMPPException e) {
			log.error(e.getMessage());
			e.printStackTrace();
			nodeRegistrationListener.onConnectToServerFailed(e.getMessage());
			return false;
		}

		return true;
	}

	@Override
	public void disconnect(){
		connection.disconnect();
		log.info("Disconnected from Jabber server.");
	}

	@Override
	public void registerNode(final String userName, final String password){
		if(!connection.isConnected()){
			nodeRegistrationListener.onNodeRegistrationFailed(userName, ErrorType.NotConnected,"Not connected to Jabber server yet.");
			return;
		}

		connection.addConnectionListener(this);

		boolean  isFailed = false;
		try {
			connection.getAccountManager().createAccount(userName, password);
		} catch (XMPPException e) {
			log.error("Error in create a new account on server: {}", e.getMessage());
			isFailed = true;
			if(e.getXMPPError().getCondition().equals(Condition.conflict.toString())){
				nodeRegistrationListener.onNodeAlreadyExisted(userName);
			} else{
				nodeRegistrationListener.onNodeRegistrationFailed(userName, ErrorType.ServerError, e.getMessage());
			}
		}

		if(isFailed) return;

		connection.addPacketListener(this, new PacketFilter() {
			@Override
			public boolean accept(Packet packet) {
				return packet instanceof Registration;
			}
		});
	}

	private String toFullUserName(String userName) {
		return userName + "@" + server + "/data";
	}

	@Override
	public void connectionClosed() {
		log.debug("Connection closed.");
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		log.error("Connection closed with error " + e.getMessage());
	}

	@Override
	public void reconnectingIn(int i) {
	}

	@Override
	public void reconnectionSuccessful() {
	}

	@Override
	public void reconnectionFailed(Exception e) {
	}

	@Override
	public void processPacket(Packet packet) {
		log.debug("Process incoming stanza:");
		log.debug(packet.toXML());

		if(!(packet instanceof Registration)) return;
		if(((Registration)packet).getAttributes() != null && ((Registration)packet).getAttributes().get("username") !=null && !((Registration)packet).getAttributes().get("username").isEmpty()){
			String message = "";
			String node = ((Registration)packet).getAttributes().get("username");
			if(processRegistrationPacket((Registration)packet, message)) {
				nodeRegistrationListener.onNodeRegistered(node);
			} else {
				nodeRegistrationListener.onNodeRegistrationFailed(node, ErrorType.PacketError, message);
			}
		}
	}

	private boolean processRegistrationPacket(Registration packet,String message) {
		//If packet type is result and there is no error, the register succeed.
		if(!packet.getType().equals(Type.RESULT)){
			message = "Wrong packet type.";
			return false;
		}

		if(packet.getError() != null){
			message = packet.getError().getMessage();
			return false;
		}

		return true;
	}
}
