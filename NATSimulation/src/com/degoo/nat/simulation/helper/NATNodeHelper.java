package com.degoo.nat.simulation.helper;

import com.degoo.protocol.NATProtos.NATMessage;
import com.degoo.protocol.CommonProtos.*;
import com.google.protobuf.ByteString;

public class NATNodeHelper {

	public static NodeID buildNodeID(long id) {
		return NodeID.newBuilder().setId(id).build();
	}

	public static NATMessage buildNATMessage(NodeID senderID, NodeID receiverID, byte[] data){
		return NATMessage.newBuilder().setSenderId(senderID).setReceiverId(receiverID).setData(ByteString.copyFrom(data)).build();
	}
}
