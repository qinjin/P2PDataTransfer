package com.degoo.nat.tests.comparison;

import com.degoo.tests.MultiProcessSimulator;
import com.degoo.nat.tests.guice.integration.MainTestRunner;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RunWith(MainTestRunner.class)
public class ComparisionTest {
	private static final Logger log = LoggerFactory.getLogger(ComparisionTest.class);
	private final MultiProcessSimulator dtmpSimulator;
	private final String testFileName;
	private Map<String, String> plainSocketNodes = new HashMap<String, String>();
	private Map<String, String> minaNodes = new HashMap<String, String>();

	@Inject
	public ComparisionTest(@Named("testFileName") String testFileName,
						   MultiProcessSimulator dtmpSimulator){
		this.testFileName = testFileName;
		this.dtmpSimulator = dtmpSimulator;
		initNodes();
	}

	private void initNodes() {
		//Just need two nodes: sender and receiver.
		plainSocketNodes.put("sender", "password");
		plainSocketNodes.put("receiver", "password");

		//MinaNode: [userId, listening port]
		minaNodes.put(String.valueOf(101), "");
		minaNodes.put(String.valueOf(201), "");
	}

	@Before
	public void setUp(){
	}

	@After
	public void tearDown() throws IOException {
		dtmpSimulator.stop(false);
	}

	@Test
	public void socketTest() throws IOException, InterruptedException {
		log.info("Started plain socket test with file: {}", testFileName);
		dtmpSimulator.start("com.degoo.nat.tests.comparison.PlainSocketNode", plainSocketNodes);
		Thread.sleep(15 *1000);
	}

	@Test
	public void minaTest() throws IOException, InterruptedException {
		log.info("Started mina test with file: {}", testFileName);
		dtmpSimulator.start("com.degoo.nat.tests.comparison.MinaNode", minaNodes);
		Thread.sleep(15 *1000);
	}
}
