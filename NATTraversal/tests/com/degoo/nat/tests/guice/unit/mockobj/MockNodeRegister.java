package com.degoo.nat.tests.guice.unit.mockobj;

import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import junit.framework.Assert;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 *
 */
public class MockNodeRegister implements ConnectionListener, PacketListener, NodeRegister {
	private final static Logger log = LoggerFactory.getLogger(MockNodeRegister.class);
	private NodeRegistrationListener listener;
	private String nodeToRegister;

	@Inject
	public MockNodeRegister(NodeRegistrationListener listener){
		this.listener = listener;
	}

	@Override
	public void connectionClosed() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void reconnectingIn(int seconds) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void reconnectionSuccessful() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void reconnectionFailed(Exception e) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void processPacket(Packet packet) {
		log.info("In mock node register processing packet");
		Assert.assertNotNull(nodeToRegister);
		listener.onNodeRegistered(nodeToRegister);
	}

	@Override
	public boolean connect() {
		log.info("In mock node register connect...");
		return true;
	}

	@Override
	public void disconnect() {
		log.info("In mock node register disconnect...");
	}

	@Override
	public void registerNode(String userName, String password) {
		log.info("In mock node register registerNode node ={}", userName);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}
		nodeToRegister = userName;
		processPacket(null);
	}
}
