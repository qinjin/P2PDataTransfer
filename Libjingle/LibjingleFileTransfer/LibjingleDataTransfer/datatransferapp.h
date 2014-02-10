#ifndef DATATRANSFERAPP_H__
#define DATATRANSFERAPP_H__

#include "tunneldatatransferclient.h"

typedef enum AppMessage{
	APP_LOGIN,
	APP_LOGOUT,
	APP_SEND
};

///////////////////////////////////////////////////////////////////////////////
// LoginInfo
///////////////////////////////////////////////////////////////////////////////
class LoginInfo{
public:
	LoginInfo(const std::string username, const std::string password, const std::string servername, int port) :
				user_name(username),
				pass_word(password),
				xmpp_server_name(servername),
				xmpp_server_port(port) {}
	const std::string Username() {return user_name;}
	const std::string Password() {return pass_word;}
	const std::string Servername() {return xmpp_server_name;}
	const int ServerPort() {return xmpp_server_port;}
private:
	const std::string user_name;
	const std::string pass_word;
	const std::string xmpp_server_name;
	const int xmpp_server_port;
};

///////////////////////////////////////////////////////////////////////////////
// DataTransferApp
///////////////////////////////////////////////////////////////////////////////
class DataTransferApp : public sigslot::has_slots<>, public talk_base::MessageHandler {
public:
	virtual void login(const std::string username, const std::string password, const std::string xmpp_server, const int server_port) = 0;
	virtual void logout() = 0;
	virtual void OnMessage(talk_base::Message *msg) = 0;
	virtual void SetDebugMode(const bool isDebug) = 0;
	virtual void ResetJingleInfo(JingleInfo jingleInfo) = 0;
	virtual talk_base::Thread* MainThread() = 0;
	virtual talk_base::SocketServer* SocketServer() = 0;
	virtual const std::string OwnID() = 0;
	virtual void AddData(const char* data, const size_t len, const std::string tunnelID ) = 0;

	//Signal will be dispatched to proxy.
	sigslot::signal1<const std::string> SignalLoggedIn;
	sigslot::signal3<const std::string, const std::string, const ErrorCode> SignalLoginFailed;
	sigslot::signal1<const std::string> SignalLoggedOut;
	sigslot::signal3<const char*, const size_t, const std::string> SignalReceived;
	sigslot::signal3<const std::string, const std::string, const ErrorCode> SignalReceiveFailed;
	sigslot::signal1<const std::string> SignalSent;
	sigslot::signal3<const std::string, const std::string, const ErrorCode> SignalSendFailed;
};

///////////////////////////////////////////////////////////////////////////////
// DataTransferAppImpl
///////////////////////////////////////////////////////////////////////////////
class DataTransferAppImpl : public DataTransferApp{
public:
	__declspec( dllexport ) DataTransferAppImpl();
	__declspec( dllexport ) ~DataTransferAppImpl();

	__declspec( dllexport ) void login(const std::string username, const std::string password, const std::string xmpp_server, const int server_port);
	__declspec( dllexport ) void logout();
	__declspec( dllexport ) void AddData(const char* data, const size_t len, const std::string tunnelID );
	__declspec( dllexport ) void SetDebugMode(const bool isDebug);
	
	void ResetJingleInfo(JingleInfo jingleInfo) { tunnel_dt_client.ResetJingleInfo(jingleInfo); }
	talk_base::Thread* MainThread(){return main_thread;}
	talk_base::SocketServer* SocketServer() {return &ss;}
	const std::string OwnID() {return tunnel_dt_client.OwnID().node();}

	void OnMessage(talk_base::Message *msg);

	//Callbacks from TunnelDataTransferClient.
	void OnDataReceived(const char* data, const size_t len, const std::string senderID);
	void OnDataSuccessfullySent(const std::string receierID);
	void OnDataSendFailed(const std::string errDesc, const ErrorCode errorCode, const std::string receiverID);
	void OnDataReceiveFailed(const std::string errDesc, const ErrorCode errorCode, const std::string senderID);
	void OnLoggedIn(const std::string id);
	void OnLoggedOut(const std::string id);

private:
	void send(const std::string tunnelID);

	XmppPump pump;
	XmppSocket* xmppSocket;
	TunnelDataTransferClient tunnel_dt_client;
	talk_base::PhysicalSocketServer ss;
	talk_base::AutoThread* main_thread;
};
#endif