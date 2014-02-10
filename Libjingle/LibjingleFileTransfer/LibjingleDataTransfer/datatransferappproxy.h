#ifndef DATATRANSFERAPPPROXY_H__
#define DATATRANSFERAPPPROXY_H__

#include"datatransferapp.h"
#include "logintimer.h"

///////////////////////////////////////////////////////////////////////////////
// ReceiverCallback
///////////////////////////////////////////////////////////////////////////////
class ReceiverCallback{
public:
	virtual ~ReceiverCallback() {}
	virtual void onLoggedIn(const std::string nodeID) {}
	virtual void onLoggedOut(const std::string nodeID) {}
	virtual void onLoginFailed(const std::string nodeID, ErrorCode errCode, const std::string errDesc) {}
	virtual void onDataReceived(const char* BYTE, const size_t len, const std::string remoteNodeID) {}
	virtual void onDataReceiveFailed(const std::string remoteNodeID, ErrorCode errCode, const std::string errDesc) {}
	virtual void onDataSent(const std::string remoteNodeID) {}
	virtual void onDataSentFailed(const std::string remoteNodeID, ErrorCode errCode, const std::string errDesc) {}
};

///////////////////////////////////////////////////////////////////////////////
// DataTransferAppProxy
///////////////////////////////////////////////////////////////////////////////
class DataTransferAppProxy : public sigslot::has_slots<>{
public:
	DataTransferAppProxy();
	~DataTransferAppProxy();
	void login(const std::string nodeID, const std::string password, const std::string xmppServer, const int port, const int time_out);
	void logout();
	void send(const char* BYTE, const size_t len, const std::string remoteNodeID);
	void setDebug(const bool isDebug);
	void setStunAndRelayInfo(const std::string stunAddr, const int stunPort, const std::string relayAddr, const int relayPort);

	void setReceiver(ReceiverCallback *cb){
		deleteReceiver();
		receiver = cb;
	}

	void deleteReceiver() {
		delete receiver;
		receiver = 0;
	}

	bool isReceiverInitialized(){
		if(!receiver){
			LOG(LERROR)<<"Receiver not initialized!";
			return false;
		}
		return true;
	}

private:
	void OnLoggedIn(const std::string id);
	void OnLoggedOut(const std::string id);
	void OnLoginFailed(const std::string id, const std::string errDesc, const ErrorCode errCode);
	void OnReceived(const char* data, const size_t dataLen, const std::string senderID);
	void OnReceiveFailed(const std::string errDesc, const std::string senderID, const ErrorCode errCode);
	void OnSent(const std::string receiverID);
	void OnSendFailed(const std::string errDesc, const std::string receiverID, const ErrorCode errCode);
	bool Validate(const std::string id);
	
	//Generate tunnel id from node id.
	const std::string createTunnelID( const std::string remoteNodeID);

	DataTransferApp* data_transfer_app;
	ReceiverCallback* receiver;
	std::string xmpp_server;
};
#endif