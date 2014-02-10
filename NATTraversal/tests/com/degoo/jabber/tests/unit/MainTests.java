package com.degoo.jabber.tests.unit;

import com.degoo.jabberclient.NodeRegistrationListener;
import com.degoo.jabberclient.ErrorType;
import com.degoo.jabberclient.NodeRegister;
import com.google.inject.Inject;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *  Test node registration and duplicated registration.
 */
@RunWith(MainTestRunner.class)
public class MainTests implements NodeRegistrationListener{
	private final static Logger log = LoggerFactory.getLogger(MainTests.class);
	private final NodeRegister nodeRegister;
	private final NodeRegistrationListener nodeRegistrationListener;
	private final Map<String, String> nodesInfoMap = new HashMap<String, String>();
	private final String TEMP_PASSWORD = "111111";
	private final int NUM_OF_NODES = 2;
	private final long TIME_OUT = 5000;
	private final Timer timer = new Timer(true);
	private final ConcurrentLinkedDeque<String> registeredNodes = new ConcurrentLinkedDeque<>();

	@Inject
	public MainTests(NodeRegister nodeRegister,
					 NodeRegistrationListener nodeRegistrationListener){
		this.nodeRegister = nodeRegister;
		this.nodeRegistrationListener = nodeRegistrationListener;
	}

	@Before
	public void setUp(){
		generateNewNodeAccounts(NUM_OF_NODES);
		connectToServer();
	}

	@After
	public void tearDown(){
		disconnectFromServer();
	}

	@Test
	public void testNodeRegistration(){
		log.debug("Start node registration tests...");
		Iterator<Entry<String, String>> entrySetIter = nodesInfoMap.entrySet().iterator();
		if(entrySetIter.hasNext()){
			Entry<String, String> entry = entrySetIter.next();
			register(entry);
			timer.schedule(new NodeRegistrationCheckTask(entry.getKey(), true), TIME_OUT);
		} else{
			log.error("No node found for register.");
		}
	}

	@Test
	public void testDuplicatedNodeRegistration(){
		log.debug("Start tests on register same node twice...");
		Iterator<Entry<String, String>> entrySetIter = nodesInfoMap.entrySet().iterator();
		if(entrySetIter.hasNext()){
			Entry<String, String> entry = entrySetIter.next();
			register(entry);
			timer.schedule(new NodeRegistrationCheckTask(entry.getKey(), true), TIME_OUT);

			//Register again.
			registeredNodes.remove(entry.getKey());
			Assert.assertEquals(false, registeredNodes.contains(entry.getKey()));

			register(entry);
			timer.schedule(new NodeRegistrationCheckTask(entry.getKey(), false), TIME_OUT);
		} else{
			log.error("No node found to register.");
		}
	}

	private void connectToServer() {
		Assert.assertTrue(nodeRegister.connect());
	}

	private void disconnectFromServer(){
		nodeRegister.disconnect();
	}

	private void register(Entry<String, String> entry){
		nodeRegister.registerNode(entry.getKey(), entry.getValue());
	}

	private void generateNewNodeAccounts(int num) {
		for(int i=0; i<num; i++){
			long now = System.currentTimeMillis();
			String nodeID = "test."+now;
			nodesInfoMap.put(nodeID, TEMP_PASSWORD);
			log.debug("Node '"+nodeID +"' generated.");
		}
	}

	@Override
	public void onNodeRegistered(String node) {
		registeredNodes.add(node);
		log.info("Node {} successfully registered.", node);
	}

	public void onNodeRegistrationFailed(String nodeID, ErrorType errorType, String reason) {
		Object[] parameters = {nodeID, errorType, reason};
		log.error("Node {} failed to register on server. error={}, reason:{}",parameters);
	}

	@Override
	public void onConnectToServerFailed(String reason) {
		log.error("Can not connect to server: {}", reason);
	}

	@Override
	public void onNodeAlreadyExisted(String node) {
		log.error("Node {} failed to register on server for duplicated registration.", node);
	}

	private class NodeRegistrationCheckTask extends TimerTask {
		private final String node;
		private final boolean expectedResult;

		public NodeRegistrationCheckTask(String node, boolean expectedResult) {
			this.node = node;
			this.expectedResult = expectedResult;
		}

		@Override
		public void run() {
			Assert.assertEquals(expectedResult, registeredNodes.contains(node));
		}
	}
}
