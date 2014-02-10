package com.degoo.nat.simulation.node;

import com.degoo.nat.simulation.helper.NATNodeHelper;
import com.degoo.protocol.CommonProtos.*;

/**
 *
 */
public class NodeInfoBean{
	private final long id;
	private final String password;

	NodeInfoBean(long id, String password){
		this.id = id;
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public NodeID getNodeID() {
		return NATNodeHelper.buildNodeID(id);
	}
}
