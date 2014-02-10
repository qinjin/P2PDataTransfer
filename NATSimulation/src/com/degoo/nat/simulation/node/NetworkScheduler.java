package com.degoo.nat.simulation.node;

import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.simulation.helper.NATNodeHelper;
import com.degoo.nat.simulation.sync.NodeStatesSynchronizer;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.tests.DataLoader;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
public class NetworkScheduler extends Thread{
	private static final Logger log = LoggerFactory.getLogger(NetworkScheduler.class);
	private final NATTraversalApp natTraversalApp;
	private final INetworkNode currentNode;
	private final int numSend;
	private final long networkDelay;
	private final long sendInterval;
	private final Timer timer;
	private final byte[] testData;
	private final NodeStatesSynchronizer nodeStatesSynchronizer;

	@Inject
	public NetworkScheduler(@Named("numSend") int numSend,
							@Named("networkDelay") long networkDelay,
							@Named("sendInterval") long sendInterval,
							@Named("testFilesDir") String testFileDir,
							@Named("testFileName") String testFileName,
							NATTraversalApp natTraversalApp,
							INetworkNode currentNode,
							DataLoader dataLoader,
							NodeStatesSynchronizer nodeStatesSynchronizer) throws IOException {
		this.numSend = numSend;
		this.sendInterval = sendInterval;
		this.networkDelay = networkDelay;
		this.natTraversalApp = natTraversalApp;
		this.currentNode = currentNode;
		this.timer = new Timer(true);
		testData = dataLoader.readData(testFileName, testFileDir);
		this.nodeStatesSynchronizer = nodeStatesSynchronizer;
	}

	public void run() {
		log.info("[Start send data]. Size = " + testData.length);
		sendData();
	}

	private void sendData() {
		//TODO: There might be more scheduling policies for multi receivers.
		final NodeID[] receivers = nodeStatesSynchronizer.getAllReceivers().toArray(new NodeID[0]);

		int nextSchedulingTime = 0;
		for (int i = 0; i < numSend; i++) {
			for (int j = 0; j < receivers.length; j++) {
				final NodeID receiver = receivers[j];
				timer.schedule(new TimerTask() {
							@Override
							public void run() {
								try {
									doSendTask(receiver);
								} catch (Exception e) {
									log.error("Exception in send data: {}" + e.getMessage());
								}
							}
						}, nextSchedulingTime);
				nextSchedulingTime += sendInterval;
			}
		}
	}

	public void doSendTask(NodeID receiver) throws InterruptedException {
		currentNode.onSend(System.currentTimeMillis(), receiver);

		//Simulate delay.
		if (networkDelay > 0) {
			Thread.sleep(networkDelay);
		}

		natTraversalApp.send(NATNodeHelper.buildNATMessage(currentNode.getNodeInfo().getNodeID(), receiver, testData));
	}

	public void login() {
		log.info("Login...");
		natTraversalApp.login(currentNode.getNodeInfo().getNodeID(), currentNode.getNodeInfo().getPassword());
	}

	public void logout() {
		log.info("[Stop send data]");
		log.info("Logged out.");
		timer.cancel();
		currentNode.generateReport();
		natTraversalApp.logout();
	}

	/**
	 * Synchronize states between nodes until all receivers are started.
	 * Note: this is a blocking call.
	 */
	public void synchronizeStates() {
		log.info("[Start synchronization]");
		nodeStatesSynchronizer.sync(currentNode.getNodeInfo().getNodeID(), natTraversalApp);
	}
}
