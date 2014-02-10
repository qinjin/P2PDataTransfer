package com.degoo.nat.simulation;

import com.degoo.util.DLLLoader;
import com.degoo.nat.simulation.guice.InjectorFactory;
import com.degoo.nat.simulation.node.NetworkNode;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;

/**
 *
 */
public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);
	private static Injector injector;

	public static void main(String[] args) {
		try {
			DLLLoader.loadLibrary(args[0]);
		} catch (UnsatisfiedLinkError error) {
			log.error("Couldn't load DLL: {}", error.getMessage());
			System.exit(-1);
		}

		injector = InjectorFactory.createInjector();
		NetworkNode networkNode = injector.getInstance(NetworkNode.class);
		printStart();
		(new ConsoleInput(networkNode)).start();
		networkNode.login();
	}

	private static void printStart() {
			System.out.println("********************************************************");
			System.out.println("              Start NAT simulation tests                ");
			System.out.println("Type 'quit': to quit simulation.                        ");
			System.out.println("********************************************************");
			System.out.println();
	}

	private static class ConsoleInput extends Thread{
		private final NetworkNode networkNode;

		public ConsoleInput(final NetworkNode networkNode){
			this.networkNode = networkNode;
		}

		public void run(){
			while(true){
				Scanner in = new Scanner(System.in);
				if(in.nextLine().equalsIgnoreCase("Quit")){
					in.close();
					networkNode.logout();
					break;
				}
			}
		}
	}
}
