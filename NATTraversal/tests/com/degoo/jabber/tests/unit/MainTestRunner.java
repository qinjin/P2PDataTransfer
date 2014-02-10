package com.degoo.jabber.tests.unit;

import com.degoo.jabber.tests.guice.NodeRegistrationModule;
import com.degoo.util.guice.GuiceTestRunner;
import org.junit.runners.model.InitializationError;

/**
 *
 */
public class MainTestRunner extends GuiceTestRunner {
	public MainTestRunner(Class classToRun) throws InitializationError {
		super(classToRun, new NodeRegistrationModule());

	}
}
