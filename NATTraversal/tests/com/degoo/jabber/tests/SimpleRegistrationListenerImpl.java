package com.degoo.jabber.tests;

import com.degoo.jabberclient.ErrorType;
import com.degoo.jabberclient.NodeRegistrationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A simple registration listener for just output registration message.
 */
public class SimpleRegistrationListenerImpl implements NodeRegistrationListener {
	private final static Logger log = LoggerFactory.getLogger(SimpleRegistrationListenerImpl.class);

	@Override
	public void onNodeRegistered(String node) {
		log.info("Node {} successfully registered.", node);
	}

	@Override
	public void onNodeRegistrationFailed(String node, ErrorType error, String reason) {
		log.error("Node {} register failed: {}", node, reason);
	}

	@Override
	public void onConnectToServerFailed(String reason) {
		log.error("Can not connect to server: {}", reason);
	}

	@Override
	public void onNodeAlreadyExisted(String node) {
		log.error("Node {} register failed for duplicated registration", node);
	}
}
