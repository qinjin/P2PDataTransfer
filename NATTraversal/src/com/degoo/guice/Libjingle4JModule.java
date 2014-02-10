package com.degoo.guice;

import com.degoo.libjingle4j.client.DataTransferClientImpl;
import com.degoo.libjingle4j.client.DataTransferListener;
import com.degoo.libjingle4j.client.IDataTransferClient;
import com.degoo.libjingle4j.client.StatusListener;
import com.degoo.nat.adapter.impl.LibjingleAdapterImpl;
import com.google.inject.AbstractModule;

/**
 *
 */
public class Libjingle4JModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(IDataTransferClient.class).to(DataTransferClientImpl.class);
		bind(StatusListener.class).to(LibjingleAdapterImpl.class);
		bind(DataTransferListener.class).to(LibjingleAdapterImpl.class);
	}
}
