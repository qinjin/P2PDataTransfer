package com.degoo.libjingle4j.client;

import com.degoo.libjingle4j.proxy.DataTransferAppProxy;
import com.degoo.libjingle4j.proxy.ErrorCode;
import com.degoo.libjingle4j.proxy.ReceiverCallback;
import com.google.inject.Inject;

/**
 *
 */
public class DataTransferClientImpl implements IDataTransferClient{
	private final ReceiverCallback dataReceiver;
	private final DataTransferAppProxy dataTransferAppProxy;

	@Inject
	public DataTransferClientImpl(StatusListener statusListener,
								  DataTransferListener transferListener,
								  DataTransferAppProxy dataTransferAppProxy){
		this.dataReceiver = new DataReceiver(statusListener, transferListener);
		this.dataTransferAppProxy = dataTransferAppProxy;
		this.dataTransferAppProxy.setReceiver(this.dataReceiver);
	}

	@Override
	public void login(String userName, String password, String serverName, int port, int timeOut) {
		dataTransferAppProxy.login(userName, password, serverName, port, timeOut);
	}

	@Override
	public void logout() {
		dataTransferAppProxy.logout();
		dataTransferAppProxy.delete();
	}

	@Override
	public void send(byte[] data, String remoteNodeID) {
		dataTransferAppProxy.send(data, data.length, remoteNodeID);
	}

	@Override
	public void setDebug(boolean isDebug) {
		dataTransferAppProxy.setDebug(isDebug);
	}

	@Override
	public void setStunAndRelayInfo(String stunAddr, int stunPort, String turnAddr, int turnPort) {
		dataTransferAppProxy.setStunAndRelayInfo(stunAddr, stunPort, turnAddr, turnPort);
	}

	/**
	 *  DataReceiver is just response for dispatch received data to listener;
	 */
	private class DataReceiver extends ReceiverCallback {
		private final DataTransferListener dataTransferListener;
		private final StatusListener statusListener;

		public DataReceiver(StatusListener statusListener,
							DataTransferListener dataTransferListener){
			super();
			this.dataTransferListener = dataTransferListener;
			this.statusListener = statusListener;
		}

		@Override
		public void onDataReceived(byte[] data, long dataLen, String remoteNodeID) {
			dataTransferListener.onDataReceived(data, dataLen, remoteNodeID);
		}

		@Override
		public void onDataReceiveFailed(String remoteNodeID, ErrorCode errCode, String errDesc) {
			dataTransferListener.onDataReceiveFailed(remoteNodeID, errCode, errDesc);
		}

		@Override
		public void onDataSent(String remoteNodeID) {
			dataTransferListener.onDataSent(remoteNodeID);
		}

		@Override
		public void onDataSentFailed(String remoteNodeID, ErrorCode errCode ,String errDesc) {
			dataTransferListener.onDataSendFailed(remoteNodeID, errCode, errDesc);
		}

		@Override
		public void onLoggedIn(String nodeID) {
			statusListener.onLoggedIn(nodeID);
		}

		@Override
		public void onLoggedOut(String nodeID) {
			statusListener.onLoggedOut(nodeID);
		}

		@Override
		public void onLoginFailed(String nodeID, ErrorCode errCode, String errDesc){
			statusListener.onLoginFailed(nodeID, errCode, errDesc);
		}

	}
}
