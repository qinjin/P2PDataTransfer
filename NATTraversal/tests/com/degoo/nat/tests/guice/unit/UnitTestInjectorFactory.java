package com.degoo.nat.tests.guice.unit;

import com.degoo.guice.PropertiesModule;
import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import com.degoo.libjingle4j.client.DataTransferListener;
import com.degoo.libjingle4j.client.IDataTransferClient;
import com.degoo.libjingle4j.client.StatusListener;
import com.degoo.nat.NATMessageCracker;
import com.degoo.nat.NATTraversalApp;
import com.degoo.nat.NodeRegistrationApp;
import com.degoo.nat.adapter.impl.LibjingleAdapterImpl;
import com.degoo.nat.adapter.NATAdapter;
import com.degoo.nat.adapter.NATAdapterListener;
import com.degoo.nat.tests.guice.ConfigPath;
import com.degoo.nat.tests.guice.unit.mockobj.*;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class UnitTestInjectorFactory {

	public static Injector createNATInjector(){
		return Guice.createInjector(
				new PropertiesModule(ConfigPath.NAT_CONF_PATH),
				new MockLibjingle4JModule(),
				new MockNATModule()
		);
	}

	public static Injector createJabberClientInjector(){
		return Guice.createInjector(
				new PropertiesModule(ConfigPath.JABBER_CONF_PATH),
				new MockJabberClientModule()
		);
	}

	private static class MockLibjingle4JModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(IDataTransferClient.class).to(MockDataTransferClient.class);
			bind(StatusListener.class).to(LibjingleAdapterImpl.class);
			bind(DataTransferListener.class).to(LibjingleAdapterImpl.class);
		}
	}


	private static class MockNATModule extends AbstractModule{
		@Override
		protected void configure() {
			bind(NATAdapter.class).to(LibjingleAdapterImpl.class);
			bind(NATMessageCracker.class).to(MockMessageCracker.class);
			bind(NATAdapterListener.class).to(NATTraversalApp.class);
			bind(NATAdapterStatusListener.class).to(NATAdapterStatusListenerImpl.class);
		}
	}

	private static class MockJabberClientModule extends AbstractModule {
		@Override
		protected void configure() {
			bind(NodeRegister.class).to(MockNodeRegister.class);
			bind(NodeRegistrationListener.class).to(NodeRegistrationApp.class);
		}
	}

}
