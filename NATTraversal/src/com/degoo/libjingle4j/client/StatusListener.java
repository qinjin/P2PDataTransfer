package com.degoo.libjingle4j.client;

import com.degoo.libjingle4j.proxy.ErrorCode;

/**
 *
 */
public interface StatusListener {
	/**
	 * Triggered when user logged in to xmpp server.
	 * @param id
	 */
	public void onLoggedIn(String id);

	/**
	 * Triggered when user logged out from xmpp server.
	 * @param id
	 */
	public void onLoggedOut(String id);

	/**
	 * Triggered when user failed login to xmpp server.
	 * @param id
	 * @param errCode
	 * @param errDesc
	 */
	public void onLoginFailed(String id, ErrorCode errCode, String errDesc);
}
