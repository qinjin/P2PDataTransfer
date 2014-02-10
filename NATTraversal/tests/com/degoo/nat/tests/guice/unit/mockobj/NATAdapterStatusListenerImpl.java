package com.degoo.nat.tests.guice.unit.mockobj;

import com.degoo.nat.tests.guice.unit.mockobj.MockMessageCracker.Status;
import com.google.inject.Singleton;

@Singleton
public class NATAdapterStatusListenerImpl implements NATAdapterStatusListener{

	private static Status natAdapterCurrentStatus;

	@Override
	public void onNewStatus(Status status) {
		this.natAdapterCurrentStatus = status;
	}

	@Override
	public Status getCurrentStatus(){
		return natAdapterCurrentStatus;
	}
}
