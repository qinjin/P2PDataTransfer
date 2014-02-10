package com.degoo.nat.simulation.node;

import com.degoo.nat.NATMessageCracker;
import com.degoo.nat.simulation.statistics.Statistics;
import com.degoo.nat.simulation.sync.NodeStatesListener;
import com.degoo.nat.simulation.sync.NodeStatesSynchronizer;
import com.degoo.nat.simulation.sync.SynchronizeListener;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NetworkErrorCode;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;

/**
 *
 */
@Singleton
public class NetworkNode implements NATMessageCracker, INetworkNode, SynchronizeListener {
	private static final Logger log = LoggerFactory.getLogger(NetworkNode.class);
	private final NetworkScheduler networkScheduler;
	private final Statistics statistics;
	private final NodeStatesListener nodeStatesListener;
	private final long nodeID;
	private final String password;

	private boolean isSyncDone = false;

	//Since the "OnSent" message for the last sending message before synchronized is always received
	// after synchronized. So we can only correctly detect whether a "OnSent" message is for sending data
	// when: 1. isSyncDone == True 2. numSendNoResponse  >= 0.
	private int numSendNoResponse = 0;

	@Inject
	public NetworkNode(@Named("nodeID") long nodeID,
					   @Named("testPassword") String password,
					   NetworkScheduler networkScheduler,
					   NodeStatesListener nodeStatesListener,
					   Statistics statistics) {
		this.nodeID = nodeID;
		this.password = password;
		this.networkScheduler = networkScheduler;
		this.statistics = statistics;
		this.nodeStatesListener = nodeStatesListener;

		statistics.setOwnID(nodeID);
	}

	public void login() {
		networkScheduler.login();
	}

	@Override
	public void onLoggedIn(NodeStatusMessage message) {
		log.debug("logged in.");
		networkScheduler.synchronizeStates();

	}

	@Override
	public void onLoggedOut(NodeStatusMessage message) {
		log.debug("logged out!");
	}

	@Override
	public void onSent(NATMessage message) {
		numSendNoResponse--;
		if (isSyncDone && numSendNoResponse >= 0) {
			statistics.updateSent(System.currentTimeMillis(), String.valueOf(message.getReceiverId().getId()), true);
			log.debug("Data sent to: {}", message.getReceiverId().getId());
		}
	}

	@Override
	public void onDataSendFailed(NATMessage message, int numFailed) {
		Object[] prameters = {numFailed, message.getReceiverId().getId(), message.getErrorCode()};
		log.error("Failed send {} data to: {} error = {}", prameters);
		//Naively update each of fails here.
		if (isSyncDone) {
			for (int i = 0; i < numFailed; i++) {
				statistics.updateSend(System.currentTimeMillis(), String.valueOf(message.getReceiverId().getId()), false);
			}
		}

	}

	@Override
	public void onReceived(NATMessage message) {
		//For receive, it might be received data before this node is synchronized since the peer node
		// would have finished synchronization. So we should compare the data to know if it is a sync message.
		if (message.getData().toStringUtf8().equals(NodeStatesSynchronizer.SYNC_REQUEST)) {
			log.debug("Recv sync request from: {}", message.getSenderId().getId());
			nodeStatesListener.sendSyncResponse(message.getSenderId());
		} else if (message.getData().toStringUtf8().equals(NodeStatesSynchronizer.SYNC_RESPONSE)) {
			log.debug("Recv sync response from: {}", message.getSenderId().getId());
			nodeStatesListener.onSyncResponse(message.getSenderId());
		} else {
			log.debug("Recv data from: {}", message.getSenderId().getId());
			statistics.updateRecv(System.currentTimeMillis(), String.valueOf(message.getSenderId().getId()), message.getErrorCode() == NetworkErrorCode.NoError);
		}
	}

	@Override
	public void onDataReceiveFailed(NATMessage message) {
		Object[] prameters = {message.getReceiverId().getId(), message.getErrorCode()};
		log.error("Failed recv data from: {} error = {}", prameters);
		// We record every receive fail but it is possible that some sync fails is received and recorded as well.
		statistics.updateRecv(System.currentTimeMillis(), String.valueOf(message.getSenderId().getId()), message.getErrorCode() != NetworkErrorCode.NoError);

	}

	@Override
	public void onLoginFailed(NodeStatusMessage message) {
		log.error("Login failed: error={}, quit...", message.getErrorCode());
		System.exit(0);
	}

	@Override
	public NodeInfoBean getNodeInfo() {
		return new NodeInfoBean(nodeID, password);
	}

	@Override
	public void generateReport() {
		statistics.generateReport();
	}

	@Override
	public void onSend(long time, NodeID receiverID) {
		//Callback from scheduler.
		numSendNoResponse++;
		if (isSyncDone) {
			statistics.updateSend(time, String.valueOf(receiverID.getId()), true);
			log.debug("Send data to: {}", receiverID);
		}
	}

	@Override
	public void onSynchronizationDone() {
		log.info("[Synchronization done]");
		isSyncDone = true;
		networkScheduler.start();
	}

	public void logout() {
		networkScheduler.logout();
	}
}
