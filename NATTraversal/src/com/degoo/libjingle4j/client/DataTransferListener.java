package com.degoo.libjingle4j.client;

import com.degoo.libjingle4j.proxy.ErrorCode;

/**
 *
 */
public interface DataTransferListener {
	public void onDataReceived(byte[] data, long dataLen, String sender);
	public void onDataReceiveFailed(String sender, ErrorCode errCode, String errDesc);
	public void onDataSent(String receiver);
	public void onDataSendFailed(String receiver, ErrorCode errCode, String errDesc);
}
