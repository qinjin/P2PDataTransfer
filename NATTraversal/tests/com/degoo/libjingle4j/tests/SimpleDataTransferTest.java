package com.degoo.libjingle4j.tests;

import com.degoo.util.DLLLoader;
import com.degoo.libjingle4j.client.DataTransferListener;
import com.degoo.libjingle4j.client.DataTransferClientImpl;
import com.degoo.libjingle4j.client.IDataTransferClient;
import com.degoo.libjingle4j.client.StatusListener;
import com.degoo.libjingle4j.proxy.DataTransferAppProxy;
import com.degoo.libjingle4j.proxy.ErrorCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A very simple test just for testing if two nodes could transfer String data.
 */
public class SimpleDataTransferTest {

	static {
		try {
			DLLLoader.load("C:\\SoftwareProjects\\Java\\Degoo\\Client\\NATTraversal\\lib\\LibjingleDataTransfer.dll");
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Native code library failed to load. See the chapter on Dynamic Linking Problems in the SWIG Java documentation for help.\n" + e);
			System.exit(1);
		}
	}

	static class LoginInfo {
		String user;
		String password;
		String serverName;
		int serverPort;
	}

	static Map<String, String> userMap;

	static {
		userMap = new HashMap<String, String>();
		userMap.put("libjingle.tester0", "111111");
		userMap.put("libjingle.tester1", "111111");
		userMap.put("libjingle.tester2", "111111");
		userMap.put("libjingle.tester3", "111111");
	}

	static class Console extends Thread {
		private IDataTransferClient dataTransferClient;

		public Console(IDataTransferClient dataTransferClient) {
			this.dataTransferClient = dataTransferClient;
			setDaemon(true);
		}

		@Override
		public void run() {
			System.out.println("*****************Available commands*********************");
			System.out.println("quit");
			System.out.println("send message remoteUser");
			System.out.println("setJingoInfo stunAddr stunPort turnAddr turnPort");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			String cmd = null;
			try {
				cmd = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			while (cmd != null && !cmd.equalsIgnoreCase("quit")) {
				if (cmd.startsWith("send")) {
					String[] sendCmd = cmd.split(" ");
					if (sendCmd.length != 3) {
						System.out.println("Error command, try again.");
					} else{
						System.out.println("Data to send out:"+ sendCmd[1]+" len="+sendCmd[1].getBytes().length);
						dataTransferClient.send(sendCmd[1].getBytes(), sendCmd[2]);
					}
				} else if(cmd.startsWith("setJingoInfo")){
					 String[] sendCmd = cmd.split(" ");
					if (sendCmd.length != 5) {
						System.out.println("Error command, try again.");
					} else{
						System.out.println("StunAddress="+ sendCmd[1]+" StunPort="+sendCmd[2]+" TurnAddress="+sendCmd[3]+" TurnPort="+sendCmd[4]);
						dataTransferClient.setStunAndRelayInfo(sendCmd[1], Integer.valueOf(sendCmd[2]), sendCmd[3], Integer.valueOf(sendCmd[4]));
					}
				} else {
					System.out.println("Error command, try again.");
				}

				try {
					cmd = in.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			dataTransferClient.logout();
			System.out.println("Quit from data transfer test.");
			System.exit(0);
		}
	}

	static class DataTransferHandler implements DataTransferListener {
		@Override
		public void onDataReceived(byte[] data, long dataLen, String sender) {
			if(data.length != dataLen){
			   System.out.println("Data missing in receiving. len="+dataLen+" received="+data.length);
			}
			System.out.println("Data received: from "+sender +" data size= "+data.length );
			for(byte b : data){
				 System.out.print((char)(b & 0xFF));
			}
			System.out.println();
		}

		@Override
		public void onDataReceiveFailed(String sender, ErrorCode errCode, String errDesc) {
			System.out.println("Data receive failed: reason = "+errDesc +", sender = "+sender +", error code="+errCode);
		}

		@Override
		public void onDataSent(String receiver) {
			System.out.println("Data sent to remote node, receiver = "+receiver );
		}

		@Override
		public void onDataSendFailed(String receiver, ErrorCode errCode, String errDesc) {
			System.out.println("Data sent failed: reason="+errDesc +", receiver= "+receiver +", error code="+errCode);
		}
	}

	static class StatusHandler implements StatusListener{

		@Override
		public void onLoggedIn(String id) {
			System.out.println("User "+ id + " logged in.");
		}

		@Override
		public void onLoggedOut(String id) {
			System.out.println("User "+ id+ "logged out.");
		}

		@Override
		public void onLoginFailed(String id, ErrorCode errCode, String errDesc) {
			System.out.println("User "+ id + " failed to login.");
		}
	}

	public static void main(String args[]) {
		DataTransferListener dataTransferHandler = new DataTransferHandler();
		StatusListener statusHandler = new StatusHandler();
		DataTransferAppProxy dataTransferAppProxy = new DataTransferAppProxy();
		IDataTransferClient dataTransferClient  = new DataTransferClientImpl(statusHandler, dataTransferHandler, dataTransferAppProxy);
		dataTransferClient.setDebug(true);
		LoginInfo loginInfo = null;
		try {
			loginInfo = getLoginInfo();
		} catch (IOException e) {
			System.out.println("Exception in getLoginInfo, app quit.");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Start login...");

		new Console(dataTransferClient).start();
		dataTransferClient.login(loginInfo.user, loginInfo.password, loginInfo.serverName, loginInfo.serverPort, 20000);
	}

	private static LoginInfo getLoginInfo() throws IOException {
		//Server and port is hardcoded.
		LoginInfo info = new LoginInfo();
		info.serverName = "localhost";
		info.serverPort = 5222;

		//Get selected user.
		String[] users = getAllUserName(userMap);
		System.out.println("Select from following users: ");
		for (int i = 0; i < users.length; i++) {
			System.out.println("[" + i + "]: " + users[i]);
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		int selection = Integer.parseInt(in.readLine());
		while (selection < 0 || selection > users.length) {
			System.out.println("Wrong selection, try again.");
			selection = Integer.parseInt(in.readLine());
		}

		info.user = users[selection];
		info.password = userMap.get(info.user);
		System.out.println("Login with user: '" + users[selection] + "' on server: '" + info.serverName + "' port:" + info.serverPort);
		return info;
	}

	private static String[] getAllUserName(Map<String, String> map) {
		String[] userNames = new String[map.size()];
		Set<String> keys = map.keySet();
		int index = 0;
		for (String key : keys) {
			userNames[index++] = key;
		}
		return userNames;
	}
}
