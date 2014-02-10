package com.degoo.nat;

import com.degoo.jabberclient.ErrorType;
import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import com.degoo.protocol.CommonProtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 *
 */
public class NodeRegistrationApp implements NodeRegistrationListener {
	private final static Logger log = LoggerFactory.getLogger(NodeRegistrationApp.class);
	private final NodeRegister nodeRegister;
	private RegistrationResult registrationResult = RegistrationResult.UnRegistered;
	private String id;

	@Inject
	public NodeRegistrationApp(NodeRegister nodeRegister){
		this.nodeRegister = nodeRegister;
	}

	/**
	 * Register node to xmpp server until success or errors.
	 */
	public RegistrationResult register(NodeID nodeID, String password, long timeOut){
		if(!nodeRegister.connect()) return registrationResult;

		String id = String.valueOf(nodeID.getId());
		this.id = id;
		nodeRegister.registerNode(id, password);

		//Waiting for registration result until timeout.
		try {
			Thread.sleep(timeOut);
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		}

		nodeRegister.disconnect();

		return registrationResult;
	}

	@Override
	public void onNodeRegistered(String nodeID) {
		if(id.equals(nodeID)) {
			log.info("Node {} registered!", nodeID);
			registrationResult = RegistrationResult.Registered;
		}
	}

	@Override
	public void onNodeRegistrationFailed(String nodeID, ErrorType errorType, String reason) {
		Object[] parameters = {nodeID, errorType, reason};
		log.error("Node {} failed to register on server. error={}, reason:{}",parameters);
	}

	@Override
	public void onConnectToServerFailed(String reason) {
		log.error("Failed to connect to server {}", reason);
	}

	@Override
	public void onNodeAlreadyExisted(String node) {
		log.error("Node {} failed to register on server for duplicated registration.", node);
		registrationResult = RegistrationResult.DuplicatedAccount;
	}
}
