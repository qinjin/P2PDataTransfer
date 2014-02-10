package com.degoo.nat.simulation.guice;

import com.degoo.guice.Libjingle4JModule;
import com.degoo.guice.NATModule;
import com.degoo.guice.PropertiesModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class InjectorFactory {
	private static final String SHARED_CONF_PATH = ".\\config\\shared.properties";
	private static final String NODE_CONF_PATH = ".\\config\\node.properties";
	private static final String NAT_CONF_PATH = ".\\config\\nat.properties";
	private static final String JABBER_CONF_PATH = ".\\config\\jabber.properties";

	public static Injector createInjector(){
		String[] configPaths = {SHARED_CONF_PATH, NODE_CONF_PATH, NAT_CONF_PATH, JABBER_CONF_PATH};

		return Guice.createInjector(
				new PropertiesModule(configPaths),
				new Libjingle4JModule(),
				new NATModule(),
				new NATMessageCrackerModule()
		);
	}
}
