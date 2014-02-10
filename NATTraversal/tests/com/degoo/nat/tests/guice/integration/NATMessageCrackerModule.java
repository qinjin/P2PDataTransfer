package com.degoo.nat.tests.guice.integration;

import com.degoo.nat.NATMessageCracker;
import com.degoo.nat.tests.integration.TestNATTraversalNode;
import com.google.inject.AbstractModule;

/**
 *
 */
public class NATMessageCrackerModule extends AbstractModule{
	@Override
	protected void configure() {
		bind(NATMessageCracker.class).to(TestNATTraversalNode.class);
	}
}
