package com.degoo.nat.simulation.node;

import com.degoo.protocol.CommonProtos.*;

/**
 *
 */
public interface NetworkSchedulerListener {
	public void onSend(long time, NodeID receiverID);
}
