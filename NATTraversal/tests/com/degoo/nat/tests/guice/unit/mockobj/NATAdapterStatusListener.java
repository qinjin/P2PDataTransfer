package com.degoo.nat.tests.guice.unit.mockobj;

import com.degoo.nat.tests.guice.unit.mockobj.MockMessageCracker.Status;

/**
 *
 */
public interface NATAdapterStatusListener {
	public void onNewStatus(Status status);
	public Status getCurrentStatus();
}
