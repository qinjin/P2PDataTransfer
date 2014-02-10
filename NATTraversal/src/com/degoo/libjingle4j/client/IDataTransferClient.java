package com.degoo.libjingle4j.client;

/**
 *    DataTransferClient will expose as a lib interface.
 */
public interface IDataTransferClient {
	public void login(String userName, String password, String serverName, int port, int timeOut);
	public void logout();
	public void send(byte[] data, String remoteNodeID);
	public void setDebug(boolean isDebug);
	public void setStunAndRelayInfo(String stunAddr, int stunPort, String turnAddr, int turnPort);
}
