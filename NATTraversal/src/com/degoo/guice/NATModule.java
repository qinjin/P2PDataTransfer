package com.degoo.guice;

import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.adapter.impl.LibjingleAdapterImpl;
import com.degoo.nat.adapter.NATAdapter;
import com.degoo.nat.adapter.NATAdapterListener;
import com.google.inject.AbstractModule;

public class NATModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(NATAdapter.class).to(LibjingleAdapterImpl.class);
		bind(NATAdapterListener.class).to(NATTraversalApp.class);
	}
}
