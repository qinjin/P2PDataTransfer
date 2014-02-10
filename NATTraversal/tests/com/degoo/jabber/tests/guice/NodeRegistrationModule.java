package com.degoo.jabber.tests.guice;

import com.degoo.jabber.tests.SimpleRegistrationListenerImpl;
import com.degoo.jabberclient.SmackNodeRegister;
import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

/**
 *
 */
public class NodeRegistrationModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(NodeRegister.class).to(SmackNodeRegister.class);
		bind(NodeRegistrationListener.class).to(SimpleRegistrationListenerImpl.class);
	}

	@Provides
	@Named("server")
	public String getServer() {
		return "localhost";
	}

	@Provides
	@Named("serverPort")
	public int getServerPort() {
		return 5222;
	}
}
