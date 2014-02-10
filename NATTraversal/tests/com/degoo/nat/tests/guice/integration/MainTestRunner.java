package com.degoo.nat.tests.guice.integration;

import com.degoo.guice.JabberClientModule;
import com.degoo.guice.Libjingle4JModule;
import com.degoo.guice.NATModule;
import com.degoo.guice.PropertiesModule;
import com.degoo.nat.tests.guice.ConfigPath;
import com.degoo.util.guice.GuiceTestRunner;
import org.junit.runners.model.InitializationError;

public class MainTestRunner extends GuiceTestRunner {

	public MainTestRunner(final Class<?> classToRun) throws InitializationError {
		super(classToRun,
				new JabberClientModule(),
				new Libjingle4JModule(),
				new NATModule(),
				new PropertiesModule(ConfigPath.JABBER_CONF_PATH, ConfigPath.NAT_CONF_PATH),
				new NATMessageCrackerModule());
	}
}
