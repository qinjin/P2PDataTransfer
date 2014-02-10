package com.degoo.nat.simulation.statistics;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 *
 */
public class StatisticsImpl implements Statistics {
	private static final Logger log = LoggerFactory.getLogger(StatisticsImpl.class);
//	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("HH.mm.ss");

	private static final String[] SENDER_REPORT_HEADER = {"receiver", "sendStartTime", "firstSentTime", "sendEndTime", "lastSentTime", "numSent", "numFailed", "numTotal"};
	private static final String[] RECEIVER_REPORT_HEADER = {"sender", "recvStartTime", "recvEndTime", "numReceived", "numFailed", "numTotal"};

	private final Map<String, StatisticBean> sendStatisticMap = new HashMap<>();
	private final Map<String, StatisticBean> recvStatisticMap = new HashMap<>();
	private final Map<String, StatisticBean> sentStatisticMap = new HashMap<>();
	private final String resultDir;
	private String nodeID;

	@Inject
	public StatisticsImpl(@Named("resultDir") String resultDir) {
		this.resultDir = resultDir;
	}

	@Override
	public void updateSend(long time, String receiver, boolean isSucceed) {
		doUpdate(sendStatisticMap, time, receiver, isSucceed);
	}

	@Override
	public void updateSent(long time, String receiver, boolean isSucceed) {
		doUpdate(sentStatisticMap, time, receiver, isSucceed);
	}

	@Override
	public void updateRecv(long time, String sender, boolean isSucceed) {
		doUpdate(recvStatisticMap, time, sender, isSucceed);
	}

	@Override
	public void setOwnID(long id) {
		nodeID = String.valueOf(id);
	}

	@Override
	public void generateReport() {
		if (nodeID == null || nodeID.isEmpty()) {
			log.warn("NodeID is unknown.");
		}
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
		String now = df.format(new Date());
		String sendReportName = resultDir + nodeID + "_" + now + "_send.csv";
		String recvReportName = resultDir + nodeID + "_" + now + "_recv.csv";
		generateSenderReport(sendReportName, sendStatisticMap, sentStatisticMap);
		generateReceiverReport(recvReportName, recvStatisticMap);
	}

	private void generateSenderReport(String fileName, Map<String, StatisticBean> sendStatisticMap, Map<String, StatisticBean> sentStatisticMap) {
		log.info("Start to generate sending report...");
		if (sendStatisticMap.isEmpty()) {
			log.error("No outgoing data found for sender report...");
			return;
		}

		try {
			List<String[]> resultList = Lists.newArrayList();

			//We handle statistics based on sendStatisticsMap since the sentStatisticMap could be empty or incomplete in case of data send failed.
			for (Entry<String, StatisticBean> entry : sendStatisticMap.entrySet()) {
				String peerID = entry.getKey();
				StatisticBean sendStatisticBean = entry.getValue();
				StatisticBean sentStatisticBean = sentStatisticMap.get(peerID);
				String[] result = new String[SENDER_REPORT_HEADER.length];

				result[0] = peerID;
				result[1] = getOutputTime(sendStatisticBean.startTime);

				result[2] = String.valueOf(sentStatisticBean != null ? getOutputTime(sentStatisticBean.startTime) : Double.NaN);
				result[3] = String.valueOf(getOutputTime(sendStatisticBean.endTime));
				result[4] = String.valueOf(sentStatisticBean != null ? getOutputTime(sentStatisticBean.endTime) : Double.NaN);
				result[5] = String.valueOf(sentStatisticBean != null ? sentStatisticBean.numSucceed : Double.NaN);
				result[6] = String.valueOf(sentStatisticBean != null ? (sendStatisticBean.numTotal - sentStatisticBean.numSucceed) : Double.NaN);
				result[7] = String.valueOf(sendStatisticBean.numTotal);
				if (sentStatisticBean != null) {
					Object[] parameters = {(sentStatisticBean.numSucceed + "/" + sendStatisticBean.numTotal), peerID, (sendStatisticBean.endTime - sendStatisticBean.startTime)};
					log.info("Total time for send num: {}, to: '{}' is {}ms.", parameters);
				} else {
					log.info("No result for data send to {}", peerID);
				}

				resultList.add(result);
			}

			writeToCSVFile(fileName, resultList, SENDER_REPORT_HEADER);
		} catch (IOException e) {
			log.error("Exception in write send report: {} ", e.getMessage());
			e.printStackTrace();
		}

		clear(sendStatisticMap);
		clear(sentStatisticMap);
	}

	private void generateReceiverReport(String fileName, Map<String, StatisticBean> recvStatisticMap) {
		log.info("Start to generate receiving report...");
		if (recvStatisticMap.isEmpty()) {
			log.error("No incoming data found for receiver report.");
			return;
		}

		try {
			List<String[]> resultList = Lists.newArrayList();

			for (Entry<String, StatisticBean> entry : recvStatisticMap.entrySet()) {
				String[] result = new String[StatisticsImpl.RECEIVER_REPORT_HEADER.length];
				String peerID = entry.getKey();
				StatisticBean statisticBean = entry.getValue();

				result[0] = peerID;
				result[1] = getOutputTime(statisticBean.startTime);
				result[2] = getOutputTime(statisticBean.endTime);
				result[3] = String.valueOf(statisticBean.numSucceed);
				result[4] = String.valueOf(statisticBean.numTotal - statisticBean.numSucceed);
				result[5] = String.valueOf(statisticBean.numTotal);

				log.info("Total time for recv from: '{}' is {}ms.", peerID, (statisticBean.endTime - statisticBean.startTime));
				resultList.add(result);
			}

			writeToCSVFile(fileName, resultList, RECEIVER_REPORT_HEADER);
		} catch (IOException e) {
			log.error("Exception in write recv report: {} ", e.getMessage());
			e.printStackTrace();
		}

		clear(recvStatisticMap);
	}

	private String getOutputTime(long time) {
		return String.valueOf(time);
	}

	private void writeToCSVFile(String fileName, List<String[]> resultList, String[] reportHeader) throws IOException {
		log.debug("Write to: {}, num records = {}", fileName, resultList.size());
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}

		CSVReader reader = new CSVReader(new FileReader(file));
		List<String[]> oldElements = reader.readAll();

		if (oldElements.isEmpty()) {
			resultList.add(0, reportHeader);
		} else {
			resultList.addAll(0, oldElements);
		}

		CSVWriter writer = new CSVWriter(new FileWriter(file));
		writer.writeAll(resultList);
		writer.flush();
		writer.close();
	}

	private void clear(Map<String, StatisticBean> map) {
		map.clear();
	}

	private void doUpdate(Map<String, StatisticBean> statisticMap, long time, String peerNode, boolean isSucceed) {
		StatisticBean statisticBean = statisticMap.get(peerNode);
		if (statisticBean == null) {
			statisticBean = new StatisticBean();
		}
		statisticBean.numTotal++;
		if (isSucceed) {
			statisticBean.numSucceed++;
			statisticBean.startTime = statisticBean.startTime < time ? statisticBean.startTime : time;
			statisticBean.endTime = statisticBean.endTime > time ? statisticBean.endTime : time;
		}
		statisticMap.put(peerNode, statisticBean);
	}


	private static class StatisticBean {
		//Total data send/received
		int numTotal = 0;
		//Number of data failed to send/recv
		int numSucceed = 0;
		//The first data successfully send/recv time.
		long startTime = Long.MAX_VALUE;
		//The last data successfully send/recv time.
		long endTime = Long.MIN_VALUE;
	}
}
