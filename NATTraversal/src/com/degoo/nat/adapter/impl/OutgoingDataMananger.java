package com.degoo.nat.adapter.impl;

import com.google.inject.Inject;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class OutgoingDataMananger{
	private final static Map<String, Integer>  pendingMsgMap = new HashMap<>();

	@Inject
	public OutgoingDataMananger(){
		refreshAll();
	}

	/**
	 * When a message is dispatch to libjingle, we increase the pending count of the receiver.
	 */
	public synchronized void addPendingCount(String receiver){
		if(pendingMsgMap.get(receiver) == null) {
			pendingMsgMap.put(receiver, 1);
		} else{
			int count = pendingMsgMap.get(receiver);
			count++;
			pendingMsgMap.put(receiver, count);
		}
	}

	/**
	 * When the message is sent to remote node, we decrease the pending count of the receiver.
	 */
	public synchronized void onMessageSent(String receiver){
		if(pendingMsgMap.get(receiver) == null) {
			throw new RuntimeException("No pending count for receiver: "+ receiver + " found.");
		}

		int count = pendingMsgMap.get(receiver);
		count --;
		if(count < 0){
			throw  new RuntimeException("Pending count is less than 0.");
		}

		pendingMsgMap.put(receiver, count);
	}

	/**
	 * Get pending count of a specified receiver.
	 */
	public synchronized int getPendingCount(String receiver){
		return pendingMsgMap.get(receiver);
	}

	/**
	 * Remove pending count of a specified receiver.
	 */
	public synchronized void removePendingCount(String receiver) {
		pendingMsgMap.remove(receiver);
	}

	private void refreshAll() {
		pendingMsgMap.clear();
	}
}
