package com.degoo.jabber.tests;

import com.degoo.jabberclient.NodeRegister;
import com.degoo.jabberclient.NodeRegistrationListener;
import com.degoo.jabberclient.SmackNodeRegister;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 *
 */
public class SimpleRegistrationTest {
	private static final ConcurrentLinkedDeque<String> registeredNodes = new ConcurrentLinkedDeque<>();

	public static void main(String[] args) throws InterruptedException {
		final NodeRegistrationListener listener = new SimpleRegistrationListenerImpl();
		if(args.length < 1){
			throw  new InterruptedException("Jabber Server name not specified!");
		}

		if(args.length <2){
			throw new InterruptedException("User to registration not specified!");
		}

		final NodeRegister register = new SmackNodeRegister(args[0], 5222, listener);
		final String user = args[1];
		final String password = "password";
		final long timeOut = 8000;

		if(!register.connect()) return;
		register.registerNode(user, password);

		//Add a timer for got registration response.
		Thread.sleep(timeOut);

		register.disconnect();
	}
}
