package com.degoo.nat.tests.integration;

import com.degoo.jabberclient.NodeRegistrationListener.RegistrationResult;
import com.degoo.nat.NodeRegistrationApp;
import com.degoo.nat.tests.guice.integration.MainTestRunner;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.tests.MultiProcessSimulator;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 *  In this test we will register several nodes, and test sending and receiving data among them concurrently.
 */
@RunWith(MainTestRunner.class)
public class MainTests {
	private final static Logger log = LoggerFactory.getLogger(MainTests.class);
	private final Map<NodeID, String> nodeToTest = new HashMap<>();
	private final Map<NodeID, String> registeredNodes = new HashMap<>();
	private final String password;
	private final NodeRegistrationApp registrationApp;
	private final MultiProcessSimulator simulator;
	private final String testFileName;
	private final long registrationTimeOut;

	@Inject
	public MainTests(@Named("numNodes") int numNodes,
					 @Named("testPassword") String password,
					 @Named("testFileName") String testFileName,
					 @Named("registrationTimeout") long registrationTimeOut,
					 NodeRegistrationApp registrationApp,
					 MultiProcessSimulator simulator) throws InterruptedException {
		this.password = password;
		this.testFileName = testFileName;
		this.registrationApp = registrationApp;
		this.simulator = simulator;
		this.registrationTimeOut = registrationTimeOut;

		generateTestNodes(nodeToTest, numNodes);
	}

	@Before
	public void setUp(){
	}

	@After
	public void tearDown() throws IOException {
		if(simulator.isStarted()){
			simulator.stop(true);
		}
	}

//	@Test
//	public void registrationTest() throws InterruptedException {
//		registerNodes();
//		Thread.sleep(20000);
//	}

	@Test
	public void transferDataConcurrentlyTest() throws IOException, InterruptedException {
		registerNodes();
		Assert.assertFalse(registeredNodes.isEmpty());

		log.info("Start concurrently data transfer test with file: '{}'", testFileName);
		simulator.start("com.degoo.nat.tests.integration.TestNATTraversalNode", toStringRepresentation(registeredNodes));

		Thread.sleep(4* 15 *1000);
	}

	private void registerNodes() {
		Iterator<Entry<NodeID, String>> iter = nodeToTest.entrySet().iterator();
		while(iter.hasNext()){
			Entry<NodeID, String> entry = iter.next();
			Assert.assertEquals(RegistrationResult.Registered, registrationApp.register(entry.getKey(), entry.getValue(), registrationTimeOut));
			registeredNodes.put(entry.getKey(), entry.getValue());
		}
		log.info("Num of {} nodes registered.", registeredNodes.size());
		Assert.assertEquals(nodeToTest.size(), registeredNodes.size());
	}

	private Map<String,String> toStringRepresentation(Map<NodeID,String> nodes) {
		Map<String, String> map = new HashMap<>();
		Iterator<Entry<NodeID, String>> iter = nodes.entrySet().iterator();
		while(iter.hasNext()){
			Entry<NodeID, String> entry = iter.next();
			map.put(String.valueOf(entry.getKey().getId()), entry.getValue());
		}

		return map;
	}

	//For num of nodes, generate nodes from (100+current million seconds) as the node id.
	private void generateTestNodes(Map<NodeID, String> nodes, int num) throws InterruptedException {
		for(int i=0; i<num; i++){
			NodeID id = NodeID.newBuilder().setId( 1000*num + System.currentTimeMillis()).build();
			log.debug("Generated node {} for test.", id.getId());
			Thread.sleep(1000);
			nodes.put(id, password);
		}

		Assert.assertFalse(nodes.isEmpty());
		Assert.assertEquals(num, nodes.size());
	}
}
