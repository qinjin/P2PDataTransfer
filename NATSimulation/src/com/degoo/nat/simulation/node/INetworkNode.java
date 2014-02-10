package com.degoo.nat.simulation.node;

/**
 *
 */
public interface INetworkNode extends NetworkSchedulerListener{
	public NodeInfoBean getNodeInfo();
	public void generateReport();
}
