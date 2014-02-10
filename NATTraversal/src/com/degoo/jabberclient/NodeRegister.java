package com.degoo.jabberclient;

/**
 *
 */
public interface NodeRegister {
	/**
	 * Connect to xmpp to server.
	 * @return  true if connected.
	 */
	public boolean connect();

	/**
	 * Disconnect from xmpp server.
	 */
	public void disconnect();

	/**
	 * Register a node to xmpp server
	 * @param userName     user name(not full name)
	 * @param password     password
	 */
	public void registerNode(final String userName,final String password);
}
