#include "datatransferclient.h"

#if defined(_MSC_VER) && (_MSC_VER < 1400)
// The following are necessary to properly link when compiling STL without
// /EHsc, otherwise known as C++ exceptions.
void __cdecl std::_Throw(const std::exception &) {}
std::_Prhand std::_Raise_handler = 0;
#endif

/*
* FileShareClient
*/
enum {
		MSG_STOP,
	};

//-----------------------------------------------------------------------------Constructor----------------------------------------------------------------------------
DataTransferClient::DataTransferClient(buzz::XmppClient *xmppclient) : xmpp_client_(xmppclient){}

//-----------------------------------------------------------------------------Destructor----------------------------------------------------------------------------
DataTransferClient::~DataTransferClient(){}

//-----------------------------------------------------------------------------Public methods--------------------------------------------------------------------------
void DataTransferClient::OnStateChange(buzz::XmppEngine::State state) {
	switch (state) {
	case buzz::XmppEngine::STATE_START:
		LOG(INFO)<< "Connecting..." << std::endl;
		break;
	case buzz::XmppEngine::STATE_OPENING:
		LOG(INFO)<< "Logging in... " << std::endl;
		break;
	case buzz::XmppEngine::STATE_OPEN:
		LOG(INFO)<< "Logged in as: " << xmpp_client_->jid().Str() << std::endl;
		OnSignon();
		break;
	case buzz::XmppEngine::STATE_CLOSED:
		LOG(INFO)<< "Logged out..." << std::endl;
		exit(0);
	}
}

void DataTransferClient::OnStatusUpdate(const buzz::Status &status) {
	const buzz::Jid remote_id = status.jid();
	if (status.available()) {
		LOG(INFO)<< "Received status update of remote node: "<<remote_id.Str() <<", it is online now.";
		node_status_map[remote_id.node()] = true;
	} else{
		LOG(INFO)<< "Received status update of remote node: "<<remote_id.Str() <<", it is offline now.";
		node_status_map[remote_id.node()] = false;
	}

	LOG(INFO)<<" Number of nodes we found: "<<node_status_map.size()<<std::endl;
}

void DataTransferClient::SendDataTo(std::string uuid, buzz::Jid remoteNodeId, char* data){
	if(node_status_map[remoteNodeId.node()]){
		session_client_->SendDataTo(uuid, &remoteNodeId, data);
		return;
	}

	LOG(INFO) <<"Failed to send: the remote node is unavailable now."<< std::endl;
}

void DataTransferClient::OnDataReceived(const char* data, size_t len){
	//TODO: handling received data to upper layer.
	LOG(INFO)<<"data received: "<< data;
}

void DataTransferClient::OnDataSuccessfullySent(std::string uuid){
	//TODO: notify upper layer a successfully sent.
}

void DataTransferClient::OnSessionError(const std::string sid, const std::string error_info){
	//TODO: handling session error.
}

std::vector<std::string> DataTransferClient::Roster(){
	std::vector<std::string> roster_v;
	std::map<const std::string, bool>::iterator iter;

	for(iter = node_status_map.begin(); iter != node_status_map.end(); iter++){
		if((*iter).second){
			roster_v.push_back((*iter).first);
		}
	}

	return roster_v;
}

//-----------------------------------------------------------------------------Private methods---------------------------------------------------------------------------
void DataTransferClient::OnJingleInfo(const std::string & relay_token,
	const std::vector<std::string> &relay_addresses,
	const std::vector<talk_base::SocketAddress> &stun_addresses) {
		port_allocator_->SetStunHosts(stun_addresses);
		port_allocator_->SetRelayHosts(relay_addresses);
		port_allocator_->SetRelayToken(relay_token);
}

void DataTransferClient::OnMessage(talk_base::Message *m) {
	//ASSERT(m->message_id == MSG_STOP);
}

void DataTransferClient::OnSignon() {
	std::string client_unique = xmpp_client_->jid().Str();							     // client_unique="user@domain/resource"

	buzz::PresencePushTask *presence_push_ = new buzz::PresencePushTask(xmpp_client_);  
	presence_push_->SignalStatusUpdate.connect(this, &DataTransferClient::OnStatusUpdate);   
	presence_push_->Start();                                                              

	buzz::Status my_status;
	my_status.set_jid(xmpp_client_->jid());
	my_status.set_available(true);
	my_status.set_show(buzz::Status::SHOW_ONLINE);
	my_status.set_priority(0);
	my_status.set_know_capabilities(true);
	//TODO: not google client!
	my_status.set_is_google_client(true);
	my_status.set_version("1.0.0.66");

	buzz::PresenceOutTask* presence_out_ = new buzz::PresenceOutTask(xmpp_client_);
	presence_out_->Send(my_status);
	presence_out_->Start();

	port_allocator_.reset(new cricket::HttpPortAllocator(&network_manager_, "pcp"));

	worker_thread_ = new talk_base::Thread();
	session_manager_.reset(new cricket::SessionManager(port_allocator_.get(), worker_thread_));
	worker_thread_->Start();

	cricket::SessionManagerTask * session_manager_task = new cricket::SessionManagerTask(xmpp_client_, session_manager_.get());
	session_manager_task->EnableOutgoingMessages();								 
    session_manager_task->Start();

	//TODO: remove jingle info task, we could set server information manually.
	buzz::JingleInfoTask *jingle_info_task = new buzz::JingleInfoTask(xmpp_client_);
	jingle_info_task->RefreshJingleInfoNow();
	jingle_info_task->SignalJingleInfo.connect(this, &DataTransferClient::OnJingleInfo);
	jingle_info_task->Start();

	session_client_ = new DataTransferSessionClient(session_manager_.get(), xmpp_client_->jid());
	session_client_->SignalDataReceived.connect(this, &DataTransferClient::OnDataReceived);
	session_client_->SignalDataSent.connect(this, &DataTransferClient::OnDataSuccessfullySent);
	session_client_->SignalSessionError.connect(this, &DataTransferClient::OnSessionError);
	session_manager_->AddClient("data_transfer", session_client_);
}