package com.degoo.nat.simulation.sync;

import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.simulation.helper.NATNodeHelper;
import com.degoo.protocol.CommonProtos.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 *
 */
@Singleton
public class SynchronizeRunnable implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(SynchronizeRunnable.class);
	private final SynchronizeListener synchronizeListener;
	private final NodeStatesListener nodeStatesListener;
	private final Timer timer;

	private NodeID senderID;
	private NATTraversalApp natTraversalApp;

	@Inject
	public SynchronizeRunnable(NodeStatesListener nodeStatesListener,
							   SynchronizeListener synchronizeListener) {
		this.nodeStatesListener = nodeStatesListener;
		this.synchronizeListener = synchronizeListener;

		this.timer = new Timer();
	}

	@Override
	public void run() {
		if (senderID == null || natTraversalApp == null) {
			log.error("SenderID or NATTraversalApp not initialized.");
			return;
		}

		scheduleNextSync();
	}

	private synchronized void scheduleNextSync() {
		timer.schedule(new TimerTask() {
					@Override
					public void run() {
						if (!nodeStatesListener.getUnSychedQueue().isEmpty()) {
							log.debug("Num of unSynced nodes are: {}", nodeStatesListener.getUnSychedQueue().size());
							for (Long receiver : nodeStatesListener.getUnSychedQueue()) {
								log.debug("Send sync request to node {}", receiver);
								NodeID receiverId = NATNodeHelper.buildNodeID(receiver);
								natTraversalApp.send(NATNodeHelper.buildNATMessage(senderID, receiverId, NodeStatesSynchronizer.SYNC_REQUEST.getBytes()));
								synchronizeListener.onSend(System.currentTimeMillis(), receiverId);
							}
							scheduleNextSync();
						}
					}
				}, 1000 * 10);
	}

	public void startSync(NodeID senderID, NATTraversalApp natTraversalApp) {
		this.senderID = senderID;
		this.natTraversalApp = natTraversalApp;

		run();
	}

	public synchronized  void sendSyncResponse(final NodeID remoteId) {
		if (senderID == null || natTraversalApp == null) {
			log.error("SenderID or NATTraversalApp not initialized.");
			return;
		}
		log.debug("Send sync response to {}", remoteId.getId());
		NodeID receiverId = NATNodeHelper.buildNodeID(remoteId.getId());
		natTraversalApp.send(NATNodeHelper.buildNATMessage(senderID, receiverId, NodeStatesSynchronizer.SYNC_RESPONSE.getBytes()));
		synchronizeListener.onSend(System.currentTimeMillis(), receiverId);
	}

	public void stopSync() {
		timer.cancel();
		synchronizeListener.onSynchronizationDone();
	}
}
