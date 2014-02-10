package com.degoo.tests;

import com.degoo.util.io.FileUtil;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

public class MultiProcessSimulator {
	private final static Logger log = LoggerFactory.getLogger(MultiProcessSimulator.class);
	private final List<Process> processes = new LinkedList<>();
	private final List<String> testResultPath = new LinkedList<>();

	private final String outputFolder;
	private final String resultFileSuffix;
	private final String logFileSuffix;
	private final String errLogFileSuffix;
	private boolean isStarted = false;

	@Inject
	public MultiProcessSimulator(@Named("outputFolder") String outputFolder,
								 @Named("resultFileSuffix") String resultFileSuffix,
								 @Named("logFileSuffix") String logFileSuffix,
								 @Named("errLogFileSuffix") String errLogFileSuffix) {
		this.outputFolder = outputFolder;
		this.resultFileSuffix = resultFileSuffix;
		this.logFileSuffix = logFileSuffix;
		this.errLogFileSuffix = errLogFileSuffix;
	}


	public void start(String classToRun, Map<String, String> nodesInfoMap) throws IOException {
		testResultPath.clear();

		Iterator<Entry<String, String>> iter = nodesInfoMap.entrySet().iterator();
		int sequenceNumber = 0;
		while (iter.hasNext()) {
			Entry<String, String> entry = iter.next();
			log.info("Node {} started.", entry.getKey());
			startNewNodeProcesses(sequenceNumber, classToRun, entry.getKey(), entry.getValue(), removeSelfAsReceiver(entry.getKey(), nodesInfoMap.keySet()));
			testResultPath.add(outputFolder + entry.getKey() + resultFileSuffix);
			sequenceNumber++;
		}

		isStarted = true;
		log.info("Total {} processes started.", nodesInfoMap.size());
	}

	//Remove node itself from receiver.
	private Set<String> removeSelfAsReceiver(final String nodeID, final Set<String> allNodeIDs) {
		Set<String> allReceivers = new HashSet<>();
		for (String node : allNodeIDs) {
			if (!node.equals(nodeID)) {
				allReceivers.add(node);
			}
		}
		return allReceivers;
	}

	public void stop(boolean isNeedProcessResult) {
		ProcessStarter.shutdown(log, processes);
		if (isNeedProcessResult) {
			processResult();
		}
		isStarted = false;
	}

	public boolean isStarted() {
		return isStarted;
	}

	private void processResult() {
		TestResultFileHandler trc = new TestResultFileHandler(testResultPath);
		trc.process();
	}

	private void startNewNodeProcesses(int sequenceNumber, String mainClass, String user, String password, Set<String> allReceivers) throws IOException {
		ProcessBuilder processBuilder = ProcessStarter.createProcess(mainClass, sequenceNumber, user, password, allReceivers, log);
		Path outputPath = createFile(user, logFileSuffix);
		processBuilder.redirectOutput(outputPath.toFile());

		Path errorPath = createFile(user, errLogFileSuffix);
		processBuilder.redirectError(errorPath.toFile());

		Process process = processBuilder.start();
		processes.add(process);
	}

	private Path createFile(String user, String logFileSuffix) {
		Path outputPath = Paths.get(outputFolder + user + logFileSuffix);
		FileUtil.createFileIfNotExists(outputPath);
		return outputPath;
	}

}
