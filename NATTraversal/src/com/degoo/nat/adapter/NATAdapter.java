package com.degoo.nat.adapter;

import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;

public interface NATAdapter {
	/**
	 * Login to server (If needed).
	 * @param id                  nodeID
	 * @param password            password
	 * @param server              serverName
	 * @param servePort           serverPort
	 * @param timeOut             time out
	 */
	public void login(NodeID id, String password, String server, int servePort, int timeOut);

	/**
	 * Logout from server.
	 */
	public void logout();

	/**
	 * Send data to another peer.
	 * @param msg                   The NetworkMessage to send
	 */
	public void send(NATMessage msg);

	/**
	 * Set STUN and TURN server info.
	 * @param stunAddr              STUN server address
	 * @param stunPort              STUN server port
	 * @param turnAddr              TURN server address
	 * @param turnPort              TURN server port
	 */
	public boolean setNATInfo(String stunAddr, int stunPort, String turnAddr, int turnPort);

	/**
	 * Set if debug (for libjingle)
	 * @param isDebug               If debug
	 */
	public void setDebugMode(boolean isDebug);
}
