package com.degoo.tests;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Result file is a one line csv file, format: {nodeID : numSend : numReceived}
 */
public class TestResultFileHandler {
	public static final String RESULT_SEPARATOR = ":";

	private final static Logger log = LoggerFactory.getLogger(TestResultFileHandler.class);
	private final List<String> resultFiles;

	public TestResultFileHandler(List<String> resultFiles) {
		this.resultFiles = resultFiles;
	}

	public void process() {
		int numTotalSent = 0;
		int numTotalReceived = 0;

		for (String resultFile : resultFiles) {
			int[] result = processFile(resultFile);
			numTotalSent += result[0];
			numTotalReceived += result[1];
		}

		log.info("[Total Result] numTotalSent={} numTotalReceived={}", numTotalSent, numTotalReceived);
		Assert.assertEquals(numTotalSent, numTotalReceived);
	}

	private int[] processFile(String resultFile) {
		String resultLine = null;
		File file = new File(resultFile);
		int[] result = {0, 0};

		if (!file.exists()) {
			log.warn("Result file '{}' not found.", resultFile);
			return result;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			resultLine = reader.readLine();
		} catch (IOException e) {
			log.error(e.getMessage());
		}


		String[] line = resultLine.split(RESULT_SEPARATOR);
		Assert.assertEquals(3, line.length);
		log.info("[Result] Node:{} sent:{} receive{}", line);

		result[0] = Integer.valueOf(line[1]);
		result[1] = Integer.valueOf(line[2]);
		return result;
	}
}
