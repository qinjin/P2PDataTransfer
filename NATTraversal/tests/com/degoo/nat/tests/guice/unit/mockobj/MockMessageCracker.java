package com.degoo.nat.tests.guice.unit.mockobj;

import com.degoo.nat.NATMessageCracker;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import com.google.inject.Inject;

/**
 *
 */
public class MockMessageCracker implements NATMessageCracker {
	private final NATAdapterStatusListener listener;

	@Inject
	public MockMessageCracker(NATAdapterStatusListener listener){
		this.listener = listener;
	}


	public enum  Status{
		OnLoggedIn,
		onLoginFailed,
		OnLoggedOut,
		OnDataReceived,
		OnDataReceiveFailed,
		OnDataSent,
		OnDataSendFailed
	}

	@Override
	public void onLoggedIn(NodeStatusMessage message) {
		listener.onNewStatus(Status.OnLoggedIn);
	}

	@Override
	public void onLoggedOut(NodeStatusMessage message) {
		listener.onNewStatus(Status.OnLoggedOut);
	}

	@Override
	public void onReceived(NATMessage message) {
		listener.onNewStatus(Status.OnDataReceived);
	}

	@Override
	public void onSent(NATMessage message) {
		listener.onNewStatus(Status.OnDataSent);
	}

	@Override
	public void onDataSendFailed(NATMessage message, int numFailed) {
		listener.onNewStatus(Status.OnDataSendFailed);
	}

	@Override
	public void onDataReceiveFailed(NATMessage message) {
		listener.onNewStatus(Status.OnDataReceiveFailed);
	}

	@Override
	public void onLoginFailed(NodeStatusMessage message) {
		listener.onNewStatus(Status.onLoginFailed);
	}
}
