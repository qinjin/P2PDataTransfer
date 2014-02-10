package com.degoo.nat.simulation.sync;

import com.degoo.protocol.CommonProtos.*;

/**
 *
 */
public interface SynchronizeListener {
	public void onSynchronizationDone();
	public void onSend(long time, NodeID receiverId);
}
