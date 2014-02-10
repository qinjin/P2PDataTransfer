package com.degoo.guice;

import com.degoo.jabberclient.SmackNodeRegister;
import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import com.degoo.nat.NodeRegistrationApp;
import com.google.inject.AbstractModule;

/**
 *
 */
public class JabberClientModule extends AbstractModule {
	@Override
	protected void configure() {
		bind(NodeRegister.class).to(SmackNodeRegister.class);
		bind(NodeRegistrationListener.class).to(NodeRegistrationApp.class);
	}
}
