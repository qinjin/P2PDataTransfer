package com.degoo.nat.simulation.sync;

import com.degoo.protocol.CommonProtos.*;

import java.util.Deque;

/**
 *
 */
public interface NodeStatesListener {
	public void onSyncResponse(NodeID senderID);

	public Deque<Long> getUnSychedQueue();

	public void sendSyncResponse(NodeID remoteId);

}
