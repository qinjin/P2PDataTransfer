package com.degoo.nat.tests.unit;

import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.tests.guice.unit.NATDataTransferTestRunner;
import com.degoo.nat.tests.guice.unit.mockobj.MockMessageCracker.Status;
import com.degoo.nat.tests.guice.unit.mockobj.NATAdapterStatusListener;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.google.protobuf.ByteString;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

@RunWith(NATDataTransferTestRunner.class)
public class DataTransferUnitTests {
	private final static Logger log = LoggerFactory.getLogger(DataTransferUnitTests.class);
	private final NATTraversalApp natTraversalApp;
	private NodeID testNode;
	private final String password = "password";
	private final NATAdapterStatusListener natAdapterStatus;

	@Inject
	public DataTransferUnitTests(NATTraversalApp natTraversalApp,
								 NATAdapterStatusListener natAdapterStatus){
		this.natTraversalApp = natTraversalApp;
		this.natAdapterStatus = natAdapterStatus;
	}

	@Before
	public void setUp(){
		testNode = NodeID.newBuilder().setId(100).build();
	}

	@Test
	public void testLogin() throws InterruptedException {
		natTraversalApp.login(testNode, password);
		Thread.sleep(2000);
		Assert.assertNotNull(natAdapterStatus.getCurrentStatus());
		Assert.assertEquals(Status.OnLoggedIn, natAdapterStatus.getCurrentStatus());
	}

	@Test
	public void testSend() throws InterruptedException {
		natTraversalApp.login(testNode, password);
		Thread.sleep(2000);
		NATMessage message = buildNetworkMessage();
		natTraversalApp.send(message);
		Thread.sleep(2000);
		Assert.assertNotNull(natAdapterStatus.getCurrentStatus());
		Assert.assertEquals(Status.OnDataSent, natAdapterStatus.getCurrentStatus());
	}

	@Test
	public void testLogout() throws InterruptedException {
		natTraversalApp.login(testNode, password);
		Thread.sleep(2000);
		natTraversalApp.logout();
		Thread.sleep(2000);
		Assert.assertNotNull(natAdapterStatus.getCurrentStatus());
		Assert.assertEquals(Status.OnLoggedOut, natAdapterStatus.getCurrentStatus());
	}

	@After
	public void tearDown(){
	}

	private NATMessage buildNetworkMessage() {
		NodeID receiver = NodeID.newBuilder().setId(101).build();
		byte[] mockData  = "testData".getBytes();
		return NATMessage.newBuilder().setSenderId(testNode).setReceiverId(receiver).setData(ByteString.copyFrom(mockData)).build();
	}
}
