package com.degoo.jabberclient;

/**
 *
 */
public interface NodeRegistrationListener {

	public enum RegistrationResult {
		Registered, UnRegistered, DuplicatedAccount
	}

	/**
	 * Called on node registered on xmpp server.
	 * @param node    The node registered.
	 */
	public void onNodeRegistered(String node);

	/**
	 * Called on node failed to register on xmpp server.
	 * @param node    The node to register.
	 * @param error   Error type.
	 * @param reason  The error description.
	 */
	public void onNodeRegistrationFailed(String node, ErrorType error, String reason);

	/**
	 * Called on node cannot connect to xmpp server.
	 * @param reason  The description of connect failed.
	 */
	public void onConnectToServerFailed(String reason);

	/**
	 * Called on register an exist node to xmpp server.
	 * @param node  The node to register.
	 */
	public void onNodeAlreadyExisted(String node);
}
