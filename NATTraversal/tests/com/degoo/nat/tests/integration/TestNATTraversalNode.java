package com.degoo.nat.tests.integration;

import com.degoo.nat.NATMessageCracker;
import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.tests.guice.integration.IntegrationTestInjectFactory;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import com.degoo.tests.DataLoader;
import com.degoo.tests.TestResultFileHandler;
import com.degoo.util.DLLLoader;
import com.degoo.util.io.FileUtil;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

/**
 *
 */
public class TestNATTraversalNode implements NATMessageCracker {
	private static final Logger log = LoggerFactory.getLogger(TestNATTraversalNode.class);
	private static Injector injector;
	private static int numDataReceived;
	private static int numDataSend;
	private static int sequenceNumber;

	private final int numNodes;
	private final int numSend;
	private final NATTraversalApp natTraversalApp;
	private final Timer timer;
	private final int dataSendInterval;
	private final long networkDelay;
	private final String outputFolder;
	private final String resultFileSuffix;
	private final String receivingFolder;
	private final boolean isSingleReceiver;
	private final DataLoader dataLoader;
	private final byte[] testData;

	private Set<NodeID> peerIDSet;
	private NodeID ownID;
	private String resultFile;

	@Inject
	public TestNATTraversalNode(@Named("numNodes") int numNodes,
								@Named("numSend") int numSend,
								@Named("dataSendInterval") int dataSendInterval,
								@Named("networkDelay") long networkDelay,
								@Named("outputFolder") String outputFolder,
								@Named("receivingFolder") String receivingFolder,
								@Named("resultFileSuffix") String resultFileSuffix,
								@Named("singleReceiver") boolean isSingleReceiver,
								@Named("testFilesDir") String testFileDir,
								@Named("testFileName") String testFileName,
								DataLoader dataLoader,
								NATTraversalApp natTraversalApp) throws IOException {
		this.numNodes = numNodes;
		this.numSend = numSend;
		this.dataSendInterval = dataSendInterval;
		this.networkDelay = networkDelay;
		this.natTraversalApp = natTraversalApp;
		this.dataLoader = dataLoader;
		this.outputFolder = outputFolder;
		this.receivingFolder = receivingFolder;
		this.resultFileSuffix = resultFileSuffix;
		this.isSingleReceiver = isSingleReceiver;
		this.timer = new Timer(true);
		this.testData = dataLoader.readData(testFileName, testFileDir);
	}

	private void initReceivers(String[] args) {
		Set<NodeID> peerIDs = new HashSet<>();
		for (int i = 3; i < args.length; i++) {
			if (!args[i].equals(args[1])) {
				peerIDs.add(NodeID.newBuilder().setId(Long.valueOf(args[i])).build());
			}
		}
		setUpOwnID(args[1]);
		setUpPeerIDSet(peerIDs);
		setUpResultFile();
	}

	public static void main(String[] args) throws InterruptedException {
		//The arg will be: sequenceNumber, username, password, receiver1, receiver2 ...
		validateArgs(args);

		try {
			DLLLoader.load("C:\\SoftwareProjects\\Java\\Degoo\\Client\\NATTraversal\\lib\\LibjingleDataTransfer.dll");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Native code library failed to load. " + e);
			System.exit(1);
		}

		//Thread.sleep(10000);

		sequenceNumber = Integer.valueOf(args[0]);

		injector = IntegrationTestInjectFactory.createNATInjector();
		TestNATTraversalNode node = injector.getInstance(TestNATTraversalNode.class);

		node.initReceivers(args);
		node.natTraversalApp.login(NodeID.newBuilder().setId(Long.valueOf(args[1])).build(), args[2]);
	}

	private void setUpOwnID(String id) {
		ownID = NodeID.newBuilder().setId(Long.valueOf(id)).build();
	}

	private void setUpPeerIDSet(Set<NodeID> peerIDSet) {
		this.peerIDSet = peerIDSet;
	}

	private void setUpResultFile() {
		this.resultFile = outputFolder + ownID.getId() + resultFileSuffix;
	}

	private static void validateArgs(String[] args) {
		if (args.length <= 2) {
			log.error("Wrong arguments numbers. Process quit!");
			System.exit(0);
		}

		try {
			Long.valueOf(args[1]);
		} catch (NumberFormatException ex) {
			log.error("Can not cast nodeID String to long value. Process quit!");
			System.exit(0);
		}
	}

	@Override
	public void onLoggedIn(NodeStatusMessage message) {
		//Start to send data out after logged in.
		log.info("[Login] Node {} logged in.", ownID.getId());

		//Waiting for a while that all nodes could logged in.
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		//Special case for two nodes, just one as a sender another as receiver:
		//The first node will act as sender, and second will act as receiver.
		if (isSingleReceiver && numNodes == 2 && sequenceNumber == 1) {
			log.info("Act as single receiver. waiting for incoming data...");
			return;
		} else if (isSingleReceiver && numNodes == 2 && sequenceNumber == 0) {
			log.info("Act as single sender. start to transfer data...");
		}

		//Schedule a timer for updating result after 40 seconds.
		new Timer(true).schedule(new ResultUpdateTask(), 40000);

		//Create tasks for sending data to random node after the time interval.
		int schedulingDelay = 0;
		for (int i = 0; i < numSend; i++) {
			timer.schedule(new DataSendTask(i), schedulingDelay);
			schedulingDelay += dataSendInterval;
		}
	}

	private class DataSendTask extends TimerTask {
		private final int index;

		public DataSendTask(int index) {
			this.index = index;
		}

		@Override
		public void run() {
			try {
				randomlySendData(index);
			} catch (Exception e) {
				log.error("Exception in send data: {}", e.getMessage());
			}
		}
	}


	private class ResultUpdateTask extends TimerTask {
		@Override
		public void run() {
			try {
				updateResultFile(resultFile, numDataSend, numDataReceived);
			} catch (IOException e) {
				log.error("Error on update result file: {}", e.getMessage());
			}
		}
	}

	private void randomlySendData(int index) {
		//For 2 nodes, single receiver, no need to get random receiver.
		NodeID receiver = isSingleReceiver && numNodes == 2 ? peerIDSet.toArray(new NodeID[0])[0] : getRandomReceiver();

		//Test data will be generated based on the test type configured!
		Object[] parameters = {receiver.getId(), System.currentTimeMillis()};
		if (index == 0) {
			log.info("[Send first data] to {} at:{}", parameters);
		} else if (index == numDataSend - 1) {
			log.info("[Send last data] to {} at:{}", parameters);
		}
		//log.debug("[Send first data] to {} at:{}", parameters);

		//Simulate link delay here.
		//Thread.sleep(networkDelay);

		natTraversalApp.send(NATMessage.newBuilder().setSenderId(ownID).setReceiverId(receiver).setData(ByteString.copyFrom(testData)).build());
	}

	private NodeID getRandomReceiver() {
		Random r = new Random();
		int selected = r.nextInt(peerIDSet.size());
		NodeID receiver = peerIDSet.toArray(new NodeID[0])[selected];
		return receiver;
	}

	@Override
	public void onLoggedOut(NodeStatusMessage message) {
		log.info("[Log out].", ownID.getId());
	}

	@Override
	public void onReceived(NATMessage message) {
		numDataReceived++;
		Object[] parameters = {message.getSenderId().getId(), System.currentTimeMillis(), numDataReceived};
		log.info("[Data Received] from {} at:{} Received={}", parameters);
		Assert.assertEquals(ownID.getId(), message.getReceiverId().getId());

		//saveToFile(message);
	}

	private void saveToFile(NATMessage message) {

		String fileName = receivingFolder + message.getSenderId().getId() + "_" + message.getReceiverId().getId() + "_" + numDataReceived + ".txt";
		FileUtil.writeSafe(Paths.get(fileName), message.getData().toByteArray());
	}

	@Override
	public void onSent(NATMessage message) {
		numDataSend++;
		Object[] parameters = {message.getReceiverId().getId(), System.currentTimeMillis(), numDataSend};
		if (numDataSend == 1) {
			log.info("[First Data Sent] to {} at:{} Sent={}", parameters);
		} else if (numDataSend == numSend) {
			log.info("[Last Data Sent] to {} at:{} Sent={}", parameters);
		}
		//log.debug("[Data Sent] to {} at:{} Sent={}", parameters);
	}

	@Override
	public void onDataSendFailed(NATMessage message, int numFailed) {
		Object[] outputArr = {numFailed, message.getReceiverId().getId(), message.getErrorCode()};
		log.info("[Data Send Failed] {} of data failed send to {}, error:{} ", outputArr);
	}

	@Override
	public void onDataReceiveFailed(NATMessage message) {
		Object[] outputArr = {message.getSenderId().getId(), message.getErrorCode()};
		log.info("[Data Receive Failed] failed receive from {}, error={}", outputArr);
	}

	@Override
	public void onLoginFailed(NodeStatusMessage message) {
		log.info("[Failed Login] Node {} failed to login. error:{}", ownID.getId(), message.getErrorCode());
	}

	private synchronized void updateResultFile(String fileName, int numSent, int numReceived) throws IOException {
		org.junit.Assert.assertNotNull(fileName);

		String result = ownID.getId() + TestResultFileHandler.RESULT_SEPARATOR + numSent + TestResultFileHandler.RESULT_SEPARATOR + numReceived;

		File resultFile = new File(fileName);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(resultFile));
			writer.write(result);
		} catch (IOException e) {
			throw e;
		} finally {
			writer.close();
		}
	}
}
