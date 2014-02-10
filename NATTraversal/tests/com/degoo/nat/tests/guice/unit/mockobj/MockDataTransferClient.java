package com.degoo.nat.tests.guice.unit.mockobj;

import com.degoo.libjingle4j.client.DataTransferListener;
import com.degoo.libjingle4j.client.IDataTransferClient;
import com.degoo.libjingle4j.client.StatusListener;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 *
 */
public class MockDataTransferClient implements IDataTransferClient{
	private final static Logger log = LoggerFactory.getLogger(MockDataTransferClient.class);
	private final StatusListener statusListener;
	private final DataTransferListener dataTransferListener;

	private String userName;

	@Inject
	public MockDataTransferClient(StatusListener statusListener,
								  DataTransferListener dataTransferListener){
		this.statusListener = statusListener;
		this.dataTransferListener = dataTransferListener;
	}

	@Override
	public void login(String userName, String password, String serverName, int port, int timeOut) {
		Assert.assertNotNull(userName);
		Assert.assertNotNull(password);
		Assert.assertNotNull(serverName);
		Assert.assertNotNull(port);
		Assert.assertFalse(userName.equals(""));
		Assert.assertFalse(password.equals(""));
		Assert.assertFalse(serverName.equals(""));
		Assert.assertTrue(port > 0 && port < 65535);

		this.userName = userName;

		Assert.assertNotNull(statusListener);
		Assert.assertNotNull(dataTransferListener);

		log.info("In mocked login...");
		statusListener.onLoggedIn(userName);
	}

	@Override
	public void logout() {
		Assert.assertNotNull(userName);
		Assert.assertFalse(userName.equals(""));
		log.info("In mocked logout...");
		statusListener.onLoggedOut(userName);
	}

	@Override
	public void send(byte[] data, String remoteNodeID) {
		Assert.assertNotNull(userName);
		Assert.assertNotNull(data);
		Assert.assertNotNull(remoteNodeID);
		Assert.assertFalse(remoteNodeID.equals(""));
		log.info("In mocked send data...");
		dataTransferListener.onDataSent(remoteNodeID);
	}

	@Override
	public void setDebug(boolean isDebug) {
		log.info("In mocked set debug data...");
		Assert.assertNotNull(isDebug);
	}

	@Override
	public void setStunAndRelayInfo(String stunAddr, int stunPort, String turnAddr, int turnPort) {
		Assert.assertNotNull(stunAddr);
		Assert.assertNotNull(stunPort);
		Assert.assertNotNull(turnAddr);
		Assert.assertNotNull(turnPort);
		log.info("In mocked set stun and turn info...");
	}
}
