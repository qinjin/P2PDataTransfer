package com.degoo.nat.tests.unit;

import com.degoo.jabberclient.NodeRegistrationListener.RegistrationResult;
import com.degoo.nat.NodeRegistrationApp;
import com.degoo.nat.tests.guice.unit.RegistrationTestsRunner;
import com.degoo.protocol.CommonProtos.*;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

/**
 *
 */
@RunWith(RegistrationTestsRunner.class)

public class RegistrationUnitTests {
	private final NodeRegistrationApp nodeRegistrationApp;

	@Inject
	public RegistrationUnitTests(NodeRegistrationApp nodeRegistrationApp){
		this.nodeRegistrationApp = nodeRegistrationApp;
	}

	@Before
	public void setUp(){
	}

	@Test
	public void registerTest(){
		NodeID nodeID = NodeID.newBuilder().setId(100).build();
		String password = "password";
		Assert.assertEquals(RegistrationResult.Registered, nodeRegistrationApp.register(nodeID, password, 1000 * 5));
	}

	@After
	public void tearDown(){
	}

}
