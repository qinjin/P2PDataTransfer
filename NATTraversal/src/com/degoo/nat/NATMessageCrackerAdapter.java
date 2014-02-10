package com.degoo.nat;

import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NodeStatusMessage;

/**
 *   An empty implement of NATMessageCracker.
 */
public class NATMessageCrackerAdapter implements NATMessageCracker{

	@Override
	public void onLoggedIn(NodeStatusMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onLoggedOut(NodeStatusMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onReceived(NATMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onSent(NATMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onDataSendFailed(NATMessage message, int numFailed) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onDataReceiveFailed(NATMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public void onLoginFailed(NodeStatusMessage message) {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
