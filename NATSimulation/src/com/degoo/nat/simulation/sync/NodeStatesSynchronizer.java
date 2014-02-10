package com.degoo.nat.simulation.sync;

import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.simulation.helper.NATNodeHelper;
import com.degoo.protocol.CommonProtos.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
@Singleton
public class NodeStatesSynchronizer implements NodeStatesListener {
	private static final Logger log = LoggerFactory.getLogger(NodeStatesSynchronizer.class);
	public final static String SYNC_REQUEST = "PING";
	public final static String SYNC_RESPONSE = "PONG";

	private final Set<NodeID> receivers;
	private final Deque<Long> unSynchedNodesQueue;
	private final SynchronizeRunnable synchronizeRunnable;
	private final long currentNodeID;

	@Inject
	public NodeStatesSynchronizer(@Named("nodeID") long nodeID,
								  @Named("receiverIDs") String receiverIDs,
								  SynchronizeRunnable synchronizeRunnable) {
		this.receivers = new HashSet<NodeID>();
		this.unSynchedNodesQueue = new ConcurrentLinkedDeque<Long>();
		this.currentNodeID = nodeID;
		this.synchronizeRunnable = synchronizeRunnable;

		initRecievers(receiverIDs);
	}

	private void initRecievers(String receiverIDs) {
		unSynchedNodesQueue.clear();
		receivers.clear();

		String[] receiversStr = receiverIDs.split(";");
		for (String receiverStr : receiversStr) {
			if (Long.valueOf(receiverStr).longValue() == currentNodeID) {
				continue;
			}
			NodeID receiver = NATNodeHelper.buildNodeID(Long.valueOf(receiverStr));
			receivers.add(receiver);
			unSynchedNodesQueue.add(receiver.getId());
		}
	}

	public Set<NodeID> getAllReceivers() {
		return receivers;
	}

	public void sync(NodeID senderID, NATTraversalApp natTraversalApp) {
		synchronizeRunnable.startSync(senderID, natTraversalApp);
	}

	@Override
	public synchronized void onSyncResponse(NodeID nodeID) {
		if (!unSynchedNodesQueue.remove(nodeID.getId())) {
			log.error("Failed to remove node {} from unsynchedQueue!", nodeID.getId());
		}

		if (unSynchedNodesQueue.isEmpty()) {
			synchronizeRunnable.stopSync();
		}
	}

	@Override
	public Deque<Long> getUnSychedQueue() {
		return unSynchedNodesQueue;
	}

	@Override
	public void sendSyncResponse(NodeID remoteId) {
		synchronizeRunnable.sendSyncResponse(remoteId);
	}
}
