package com.degoo.tests;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

class ProcessStarter {
	//Num of peers shouldn't be large, otherwise the command send to new process will be overflow...
	//This can be modified to a proper way later, but with a lower priority.
	//Args of new process are: numberStarted, user, password, receiver1, receiver2...
	static ProcessBuilder createProcess(String mainClass, int sequenceNumber, String user, String password, Set<String> allReceivers, Logger log) {
		String jvm = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		log.debug("JVM: " + jvm);
		String classpath = System.getProperty("java.class.path");
		//Use System.load, so no need to specify native library.
//		String vmOption = "-Djava.library.path=C:" + File.separator +"SoftwareProjects" + File.separator +"Java "+ File.separator
//				+"Degoo"+ File.separator +"Client"+ File.separator +"Libjingle"+ File.separator +"Libjingle4j"+ File.separator +"lib";
		String vmMaxHeap = "-Xmx1024M";
		List<String> commands = new ArrayList<>();
		commands.add(jvm);
		commands.add(vmMaxHeap);
//		commands.add(vmOption);
		commands.add(mainClass);
		commands.add(String.valueOf(sequenceNumber));
		commands.add(user);
		commands.add(password);

		for (String nodeID : allReceivers) {
			commands.add(nodeID);
		}

		ProcessBuilder processBuilder = new ProcessBuilder(commands);
		processBuilder.inheritIO();
		Map<String, String> environment = processBuilder.environment();
		environment.put("CLASSPATH", classpath);

		return processBuilder;
	}


	private static void killProcess(final Process process) {
		process.destroy();
	}

	public static void shutdown(Logger log, List<Process> processes) {
		log.info("Killing " + processes.size() + " processes.");
		for (Process process : processes) {
			killProcess(process);
		}
	}
}
