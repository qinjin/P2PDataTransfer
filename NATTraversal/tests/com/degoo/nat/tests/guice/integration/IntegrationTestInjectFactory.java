package com.degoo.nat.tests.guice.integration;

import com.degoo.guice.Libjingle4JModule;
import com.degoo.guice.NATModule;
import com.degoo.guice.PropertiesModule;
import com.degoo.nat.tests.guice.ConfigPath;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 *
 */
public class IntegrationTestInjectFactory {
	public static Injector createNATInjector(){
		String[] configPaths = {ConfigPath.JABBER_CONF_PATH, ConfigPath.NAT_CONF_PATH};
		return Guice.createInjector(
				new Libjingle4JModule(),
				new NATModule(),
				new PropertiesModule(configPaths),
				new NATMessageCrackerModule()
		);
	}
}
