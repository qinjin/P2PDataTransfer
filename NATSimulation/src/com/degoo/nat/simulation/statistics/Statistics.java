package com.degoo.nat.simulation.statistics;

/**
 *
 */
public interface Statistics {
	public void updateRecv(long time, String sender, boolean isSucceed);
	public void updateSend(long time, String receiver, boolean isSucceed);
	public void updateSent(long time, String receiver, boolean isSucceed);
	public void generateReport();
	public void setOwnID(long id);
}
