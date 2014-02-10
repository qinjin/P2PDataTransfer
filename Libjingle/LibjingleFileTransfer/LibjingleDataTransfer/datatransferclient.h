#ifndef DATATRANSFER_CLIENT_H__
#define DATATRANSFER_CLIENT_H__

#include "datatransfer.h"

typedef std::map<const std::string, bool> NodeStatusMap;

class DataTransferClient : public sigslot::has_slots<>, public talk_base::MessageHandler{
public:
	DataTransferClient(buzz::XmppClient* xmppclient);
	~DataTransferClient();

	//Triggered when received login state from server.
	void OnStateChange(buzz::XmppEngine::State state);

	//Triggered when received roster info from server.
	void OnStatusUpdate(const buzz::Status &status);

	//Send data to remote node.
	void SendDataTo(std::string uuid, buzz::Jid remoteJID, char* data);

	//Triggered when received data from remote node.
	void OnDataReceived(const char* data, size_t len);

	//Triggered when remote node received data.
	void OnDataSuccessfullySent(std::string uuid);

	//Triggered when session error occurred.
	void OnSessionError(const std::string sid, const std::string error_info);

	//
	void SetTimeOut(long timeOut){time_out = timeOut;}

	//
	long TimeOut(){return time_out;}

	std::vector<std::string> Roster();

	sigslot::signal2<std::string, const char*> SignalDataReceived;
	sigslot::signal1<std::string> SignalDataSuccessfullySent;
	sigslot::signal1<std::string> SignalDataSentFailed;

private:
	//Triggered when received stun&turn info from server.
	void OnJingleInfo(const std::string & relay_token, const std::vector<std::string> &relay_addresses, const std::vector<talk_base::SocketAddress> &stun_addresses);
	
	//Triggered when signed in to server.
	void OnSignon();

	//Triggered when received message from another thread.
	void OnMessage(talk_base::Message *m);

	talk_base::NetworkManager network_manager_;                         
	talk_base::scoped_ptr<cricket::HttpPortAllocator> port_allocator_;
	talk_base::scoped_ptr<cricket::SessionManager> session_manager_;
	DataTransferSessionClient* session_client_;
	buzz::XmppClient *xmpp_client_;
	talk_base::Thread* worker_thread_;
	NodeStatusMap node_status_map;
	long time_out;
};

#endif;