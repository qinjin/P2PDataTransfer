package com.degoo.nat.adapter.impl;

import com.degoo.libjingle4j.proxy.ErrorCode;
import com.degoo.protocol.CommonProtos.*;
import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.NATProtos.NetworkErrorCode;
import com.degoo.protocol.NATProtos.NodeStatusMessage;
import com.google.protobuf.ByteString;

/**
 *
 */
class SessionHelper {
	/**
	 * Convert from libjingle message to node status message, like: {nodeID, status, NetworkMessageStatusCode.Succeed}.
	 */
	public NodeStatusMessage convertToStatusMessage(NodeID nodeID, boolean onlineStatus) {
		return NodeStatusMessage.newBuilder().setNodeId(nodeID).setErrorCode(NetworkErrorCode.NoError).build();
	}

	/**
	 * Convert from libjingle message to failed login message, like: {nodeID, status, NetworkMessageStatusCode}.
	 */
	public NodeStatusMessage convertToLoginFailedStatusMessage(NodeID nodeID, ErrorCode errorCode) {
		return NodeStatusMessage.newBuilder().setNodeId(nodeID).setErrorCode(fromLibjingleErrorCodes(errorCode)).build();
	}

	/**
	 * Convert from libjingle sent message to network message, like: {networkMessageType, senderID, receiverID, data, NetworkMessageStatusCode.Succeed}.
	 */
	public NATMessage convertToSentMessage(NodeID senderId, NodeID receiverID) throws NumberFormatException{
		return NATMessage.newBuilder().setSenderId(senderId).setReceiverId(receiverID).setErrorCode(NetworkErrorCode.NoError).build();
	}

	/**
	 * Convert from libjingle received message to network message, like: {networkMessageType, senderID, receiverID, data, NetworkMessageStatusCode.Succeed}.
	 */
	public NATMessage convertToReceivedMessage(byte[] data, NodeID senderID, NodeID receiverID) throws NumberFormatException{
		return NATMessage.newBuilder().setSenderId(senderID).setReceiverId(receiverID).setData(ByteString.copyFrom(data)).setErrorCode(NetworkErrorCode.NoError).build();
	}

	/**
	 *  Convert from libjingle receive failed message to network message, like: {networkMessageType, senderID,  ,  , NetworkMessageStatusCode}.
	 */
	public NATMessage convertToReceiveFailedMessage(ErrorCode errCode, NodeID senderID) throws NumberFormatException {
		return NATMessage.newBuilder().setSenderId(senderID).setErrorCode(fromLibjingleErrorCodes(errCode)).build();
	}

	/**
	 *  Convert from libjingle send failed message to network message, like: {networkMessageType,  , receiverID, , NetworkMessageStatusCode}.
	 */
	public NATMessage convertToSendFailedMessage(NATMessage message, ErrorCode errCode) throws NumberFormatException {
		return message.toBuilder().setErrorCode(fromLibjingleErrorCodes(errCode)).build();
	}

	/**
	 * Convert from libjingle error to NAT error types.
	 * @param errCode                Error code in libjingle.
	 * @return                       NATTraversal error code.
	 * @throws RuntimeException      If libjingle error code is unknown.
	 */
	private NetworkErrorCode fromLibjingleErrorCodes(ErrorCode errCode) throws RuntimeException {
		switch(errCode){
			case InvalidReceiver:
				return NetworkErrorCode.InvalidReceiver;
		    case MalformedID:
				return NetworkErrorCode.MalformedID;
			case StreamWriteError:
			case StreamReadError:
				return NetworkErrorCode.StreamErrors;
			case ReceiverUnavailable:
				return NetworkErrorCode.ReceiverUnavailable;
			case NotifiedTunnelErrors:
				return NetworkErrorCode.NotifiedTunnelErrors;
			case SessionTimeOut:
				return NetworkErrorCode.SessionTimeOut;
			case SessionErrors:
				return NetworkErrorCode.SessionErrors;
			case LoginTimeout:
				return NetworkErrorCode.ServerNotConnected;
			default:
				throw new RuntimeException("Unknown network error code: "+errCode);
		}
	}
}
