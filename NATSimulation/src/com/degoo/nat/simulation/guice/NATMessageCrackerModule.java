package com.degoo.nat.simulation.guice;

import com.degoo.nat.NATMessageCracker;
import com.degoo.nat.simulation.node.INetworkNode;
import com.degoo.nat.simulation.node.NetworkNode;
import com.degoo.nat.simulation.node.NetworkSchedulerListener;
import com.degoo.nat.simulation.statistics.Statistics;
import com.degoo.nat.simulation.statistics.StatisticsImpl;
import com.degoo.nat.simulation.sync.NodeStatesListener;
import com.degoo.nat.simulation.sync.NodeStatesSynchronizer;
import com.degoo.nat.simulation.sync.SynchronizeListener;
import com.google.inject.AbstractModule;

/**
 *
 */
class NATMessageCrackerModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(NATMessageCracker.class).to(NetworkNode.class);
		bind(NetworkSchedulerListener.class).to(NetworkNode.class);
		bind(Statistics.class).to(StatisticsImpl.class);
		bind(NodeStatesListener.class).to(NodeStatesSynchronizer.class);
		bind(INetworkNode.class).to(NetworkNode.class);
		bind(SynchronizeListener.class).to(NetworkNode.class);
	}
}

