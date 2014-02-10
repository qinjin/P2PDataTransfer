package com.degoo.nat;

import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NodeStatusMessage;

/**
 *  NAT traversal app call back API.
 */
public interface NATMessageCracker {
	public void onLoggedIn(NodeStatusMessage message);
	public void onLoggedOut(NodeStatusMessage message);
	public void onReceived(NATMessage message);
	public void onSent(NATMessage message);
	public void onDataSendFailed(NATMessage message, int numFailed);
	public void onDataReceiveFailed(NATMessage message);
	public void onLoginFailed(NodeStatusMessage message);
}
