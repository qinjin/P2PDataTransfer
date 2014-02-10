package com.degoo.nat.adapter;

import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NodeStatusMessage;

/**
 *
 */
public interface NATAdapterListener {
	public void onLoggedIn(NodeStatusMessage message);
	public void onLoggedOut(NodeStatusMessage message);
	public void onLoginFailed(NodeStatusMessage message);
	public void onDataReceived(NATMessage message);
	public void onDataReceiveFailed(NATMessage message);
	public void onDataSent(NATMessage message);
	public void onDataSendFailed(NATMessage message, int numFailed);
}
