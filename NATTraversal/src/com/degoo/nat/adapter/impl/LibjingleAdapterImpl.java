/*
 * Copyright (c) 2011. Degoo Media AB
 */

package com.degoo.nat.adapter.impl;

import com.degoo.libjingle4j.client.DataTransferListener;
import com.degoo.libjingle4j.client.IDataTransferClient;
import com.degoo.libjingle4j.client.StatusListener;
import com.degoo.libjingle4j.proxy.ErrorCode;
import com.degoo.nat.adapter.NATAdapter;
import com.degoo.nat.adapter.NATAdapterListener;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NetworkErrorCode;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  This class using libjingle library to connect/send message/disconnect to remote node.
 */
public class LibjingleAdapterImpl implements NATAdapter, StatusListener, DataTransferListener {
	private final static Logger log = LoggerFactory.getLogger(LibjingleAdapterImpl.class);
	private final IDataTransferClient dataTransferClient;
	private final NATAdapterListener natAdapterListener;
	private final SessionHelper sessionHelper;
	private final OutgoingDataMananger outgoingDataMananger;
	private final Timer loginTimer;
	private boolean isLoggedIn;
	private boolean isLoginFailed;
	private NodeID ownID = null;

	@Inject
	public LibjingleAdapterImpl(OutgoingDataMananger outgoingDataMananger,
								IDataTransferClient dataTransferClient,
								NATAdapterListener natAdapterListener,
								SessionHelper sessionHelper){
		this.outgoingDataMananger = outgoingDataMananger;
		this.dataTransferClient = dataTransferClient;
		this.natAdapterListener = natAdapterListener;
		this.sessionHelper = sessionHelper;
		loginTimer = new Timer(true);
		isLoggedIn = false;
		isLoginFailed = false;
	}

	@Override
	public void login(NodeID nodeID, String password, String server, int servePort, int timeOut) {
		//Fast notify user the login failed if the nodeID is not init.
		if(nodeID == null || !nodeID.isInitialized()){
			natAdapterListener.onLoginFailed(NodeStatusMessage.newBuilder().setNodeId(nodeID).setErrorCode(NetworkErrorCode.MalformedID).build());
			return;
		}

		this.ownID = nodeID;

		loginTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				if(!isLoggedIn) {
					onLoginFailed(String.valueOf(ownID.getId()), ErrorCode.LoginTimeout, "Login timeout");
				}
			}
		}, timeOut);

		XmppSignallingThread signallingThread = new XmppSignallingThread(dataTransferClient, ownID, password, server, servePort, timeOut);
		signallingThread.run();
	}

	@Override
	public void logout() {
		dataTransferClient.logout();
	}

	@Override
	public void send(NATMessage message){
		if(ownID == null){
			throw new RuntimeException("Null local node id. Is send data before logged in to server?");
		}

		//Check if sender id and receiver id is valid.
		NetworkErrorCode errorCode = validateNATMessage(message);
		if(errorCode != NetworkErrorCode.NoError) {
			natAdapterListener.onDataSendFailed(NATMessage.newBuilder().setSenderId(message.getSenderId()).setReceiverId(message.getReceiverId()).setErrorCode(errorCode).build(), 1);
			return;
		}

		String receiver = String.valueOf(message.getReceiverId().getId());
		outgoingDataMananger.addPendingCount(receiver);
		dataTransferClient.send(message.getData().toByteArray(), receiver);
	}

	@Override
	public boolean setNATInfo(String stunAddr, int stunPort, String turnAddr, int turnPort) {
		if(!validateNATInfo(stunAddr, stunPort, turnAddr, turnPort)) {
			return false;
		}

		dataTransferClient.setStunAndRelayInfo(stunAddr, stunPort, turnAddr, turnPort);
		return true;
	}

	@Override
	public void setDebugMode(boolean isDebug) {
		dataTransferClient.setDebug(isDebug);
	}

	@Override
	public void onLoggedIn(String id) {
		//In case the login failed for timeout but the node actually login. We will ignore this login, and
		//try another login with a larger timeout.
		if(isLoginFailed){
			return;
		}

		try {
			isLoggedIn = true;
			natAdapterListener.onLoggedIn(sessionHelper.convertToStatusMessage(toNodeID(id), true));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onLoggedOut(String id) {
		try {
			natAdapterListener.onLoggedOut(sessionHelper.convertToStatusMessage(toNodeID(id), false));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onLoginFailed(String id, ErrorCode errCode, String errDesc) {
		log.error("Failed to login to xmpp server, reason: {}", errDesc);
		try {
			isLoginFailed = true;
			natAdapterListener.onLoginFailed(sessionHelper.convertToLoginFailedStatusMessage(toNodeID(id), errCode));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onDataReceived(byte[] data, long dataLen, String sender) {
		try {
			natAdapterListener.onDataReceived(sessionHelper.convertToReceivedMessage(data, toNodeID(sender), ownID));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onDataReceiveFailed(String sender, ErrorCode errCode, String errDesc) {
		log.error("Error on receive data from {} reason: {}", sender ,errDesc);
		try {
			natAdapterListener.onDataReceiveFailed(sessionHelper.convertToReceiveFailedMessage(errCode, toNodeID(sender)));
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onDataSent(String receiver) {
		try {
			natAdapterListener.onDataSent(sessionHelper.convertToSentMessage(ownID, toNodeID(receiver)));
			outgoingDataMananger.onMessageSent(receiver);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void onDataSendFailed(String receiver, ErrorCode errCode, String errDesc) {
		Object[] parameters = {receiver, errDesc, errCode};
		log.error("Error on send data to {} reason: {} error code = {}", parameters);
		try {
			int failedCount = outgoingDataMananger.getPendingCount(receiver);
			natAdapterListener.onDataSendFailed(sessionHelper.convertToSendFailedMessage(NATMessage.newBuilder().setSenderId(ownID).setReceiverId(toNodeID(receiver)).build(), errCode), failedCount);
			outgoingDataMananger.removePendingCount(receiver);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}

	private boolean validateNATInfo(String stunAddr, int stunPort, String turnAddr, int turnPort) {
		return stunAddr != null && !stunAddr.isEmpty() && stunPort > 0 && stunPort < 65536
				&& turnAddr != null && !turnAddr.isEmpty() && turnPort > 0 && turnPort < 65536;
	}

	private NetworkErrorCode validateNATMessage(NATMessage message) {
		if(message.getSenderId() == null || !message.getSenderId().isInitialized() || message.getSenderId().getId() <= 0){
			log.error("Sender id is not valid.");
			return NetworkErrorCode.MalformedID;
		} else if(message.getReceiverId() == null || !message.getReceiverId().isInitialized() || message.getReceiverId().getId() <= 0) {
			log.error("Receiver id is not valid.");
			return NetworkErrorCode.MalformedID;
		}
		return NetworkErrorCode.NoError;
	}

	private NodeID toNodeID(String id) throws RuntimeException{
		if(id == null || id.isEmpty()) throw new RuntimeException("Malformed node id: {"+ id +"}");
		return NodeID.newBuilder().setId(Long.valueOf(id)).build();
	}

	private class XmppSignallingThread implements Runnable{
		private final IDataTransferClient dataTransferClient;
		private final NodeID ownID;
		private final String password;
		private final String server;
		private final int serverPort;
		private final int timeOut;

		public XmppSignallingThread(IDataTransferClient dataTransferClient, NodeID nodeID, String password, String server, int serverPort, int timeOut) {
			this.dataTransferClient = dataTransferClient;
			this.ownID = nodeID;
			this.password = password;
			this.server = server;
			this.serverPort = serverPort;
			this.timeOut = timeOut;
		}

		//XmppSignallingThread will login and blocked for xmpp signaling messages.
		@Override
		public void run() {
			dataTransferClient.login(String.valueOf(ownID.getId()), password, server, serverPort, timeOut);
		}
	}
}
