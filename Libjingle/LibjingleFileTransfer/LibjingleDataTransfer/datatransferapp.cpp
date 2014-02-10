#include "datatransferapp.h"

///////////////////////////////////////////////////////////////////////////////
// DebugLog
///////////////////////////////////////////////////////////////////////////////
class DebugLog : public sigslot::has_slots<> {
public:
	DebugLog() :
	  debug_input_buf_(NULL), debug_input_len_(0), debug_input_alloc_(0),
		  debug_output_buf_(NULL), debug_output_len_(0), debug_output_alloc_(0),
		  censor_password_(false)
	  {}
	  char * debug_input_buf_;
	  int debug_input_len_;
	  int debug_input_alloc_;
	  char * debug_output_buf_;
	  int debug_output_len_;
	  int debug_output_alloc_;
	  bool censor_password_;

	  void Input(const char * data, int len) {
		  if (debug_input_len_ + len > debug_input_alloc_) {
			  char * old_buf = debug_input_buf_;
			  debug_input_alloc_ = 4096;
			  while (debug_input_alloc_ < debug_input_len_ + len) {
				  debug_input_alloc_ *= 2;
			  }
			  debug_input_buf_ = new char[debug_input_alloc_];
			  memcpy(debug_input_buf_, old_buf, debug_input_len_);
			  delete[] old_buf;
		  }
		  memcpy(debug_input_buf_ + debug_input_len_, data, len);
		  debug_input_len_ += len;
		  DebugPrint(debug_input_buf_, &debug_input_len_, false);
	  }

	  void Output(const char * data, int len) {
		  if (debug_output_len_ + len > debug_output_alloc_) {
			  char * old_buf = debug_output_buf_;
			  debug_output_alloc_ = 4096;
			  while (debug_output_alloc_ < debug_output_len_ + len) {
				  debug_output_alloc_ *= 2;
			  }
			  debug_output_buf_ = new char[debug_output_alloc_];
			  memcpy(debug_output_buf_, old_buf, debug_output_len_);
			  delete[] old_buf;
		  }
		  memcpy(debug_output_buf_ + debug_output_len_, data, len);
		  debug_output_len_ += len;
		  DebugPrint(debug_output_buf_, &debug_output_len_, true);
	  }

	  static bool
		  IsAuthTag(const char * str, size_t len) {
			  if (str[0] == '<' && str[1] == 'a' &&
				  str[2] == 'u' &&
				  str[3] == 't' &&
				  str[4] == 'h' &&
				  str[5] <= ' ') {
					  std::string tag(str, len);

					  if (tag.find("mechanism") != std::string::npos)
						  return true;

			  }
			  return false;
	  }

	  void
		  DebugPrint(char * buf, int * plen, bool output) {
			  int len = *plen;
			  if (len > 0) {
				  time_t tim = time(NULL);
				  struct tm * now = localtime(&tim);
				  char *time_string = asctime(now);
				  if (time_string) {
					  size_t time_len = strlen(time_string);
					  if (time_len > 0) {
						  time_string[time_len-1] = 0;    // trim off terminating \n
					  }
				  }
				  LOG(INFO) << (output ? "SEND >>>>>>>>>>>>>>>>>>>>>>>>>" : "RECV <<<<<<<<<<<<<<<<<<<<<<<<<")
					  << " : " << time_string;

				  bool indent;
				  int start = 0, nest = 3;
				  for (int i = 0; i < len; i += 1) {
					  if (buf[i] == '>') {
						  if ((i > 0) && (buf[i-1] == '/')) {
							  indent = false;
						  } else if ((start + 1 < len) && (buf[start + 1] == '/')) {
							  indent = false;
							  nest -= 2;
						  } else {
							  indent = true;
						  }

						  // Output a tag
						  LOG(INFO) << std::setw(nest) << " " << std::string(buf + start, i + 1 - start);

						  if (indent)
							  nest += 2;

						  // Note if it's a PLAIN auth tag
						  if (IsAuthTag(buf + start, i + 1 - start)) {
							  censor_password_ = true;
						  }

						  // incr
						  start = i + 1;
					  }

					  if (buf[i] == '<' && start < i) {
						  if (censor_password_) {
							  LOG(INFO) << std::setw(nest) << " " << "## TEXT REMOVED ##";
							  censor_password_ = false;
						  }
						  else {
							  LOG(INFO) << std::setw(nest) << " " << std::string(buf + start, i - start);
						  }
						  start = i;
					  }
				  }
				  len = len - start;
				  memcpy(buf, buf + start, len);
				  *plen = len;
			  }
	  }

};

static DebugLog debug_log_;

///////////////////////////////////////////////////////////////////////////////
// DataTransferAppImpl
///////////////////////////////////////////////////////////////////////////////
DataTransferAppImpl::DataTransferAppImpl() :
		pump(XmppPump()),
		ss(talk_base::PhysicalSocketServer()),
		main_thread(new talk_base::AutoThread(&ss)),
		tunnel_dt_client(TunnelDataTransferClient(pump.client(), false)) {

	tunnel_dt_client.SignalDataReceived.connect(this, &DataTransferAppImpl::OnDataReceived);
	tunnel_dt_client.SignalDataSuccessfullySent.connect(this, &DataTransferAppImpl::OnDataSuccessfullySent);
	tunnel_dt_client.SignalDataSentFailed.connect(this, &DataTransferAppImpl::OnDataSendFailed);
	tunnel_dt_client.SignalDataReceiveFailed.connect(this, &DataTransferAppImpl::OnDataReceiveFailed);
	tunnel_dt_client.SignalLoggedIn.connect(this, &DataTransferAppImpl::OnLoggedIn);
	tunnel_dt_client.SignalLoggedOut.connect(this, &DataTransferAppImpl::OnLoggedOut);

#if 0
	LOG(INFO) << "Main thread name = " << main_thread->name();
#endif//DEBUG MSG
}

void DataTransferAppImpl::SetDebugMode(const bool isDebug){
	if (isDebug){
		talk_base::LogMessage::LogToDebug(talk_base::LS_INFO);
		pump.client()->SignalLogInput.connect(&debug_log_, &DebugLog::Input);
		pump.client()->SignalLogOutput.connect(&debug_log_, &DebugLog::Output);
	} else
		talk_base::LogMessage::LogToDebug(talk_base::LS_WARNING);
}

void DataTransferAppImpl::login(const std::string username, const std::string password, const std::string xmpp_server_name, const int port) {
	buzz::Jid jid(username, xmpp_server_name, JID_RESOURCE);
	buzz::XmppClientSettings xclientSettings;

	if(!jid.IsValid() || jid.node() == ""){
		std::string errDesc = "Invalid JID. Node id or server name is missing.";
		LOG(LERROR) << errDesc;
		SignalLoginFailed(jid.node(), errDesc, MalformedID);
		return;
	}

	xclientSettings.set_user(jid.node());
	xclientSettings.set_resource("data");
	xclientSettings.set_host(jid.domain());
	xclientSettings.set_allow_plain(true);
	xclientSettings.set_use_tls(false);
	xclientSettings.set_server(talk_base::SocketAddress(xmpp_server_name, port));

	talk_base::InsecureCryptStringImpl pass;
	pass.password() = password;
	xclientSettings.set_pass(talk_base::CryptString(pass));

	tunnel_dt_client.SetXMPPServer(xmpp_server_name);
	pump.client() -> SignalStateChange.connect(&tunnel_dt_client, &TunnelDataTransferClient::OnStateChange);

	talk_base::ThreadManager::SetCurrent(main_thread);

	LOG(INFO) << "started doLogin"<< std::endl;

	xmppSocket = new XmppSocket(true);
	pump.DoLogin(xclientSettings, xmppSocket, NULL);

	if(!MainThread()->started()){
		MainThread()->Run();
	}
}

void DataTransferAppImpl::logout(){
	pump.DoDisconnect();
}

void DataTransferAppImpl::AddData(const char* data, const size_t len, const std::string tunnelID ) {
	tunnel_dt_client.AddData(data, len, tunnelID);
	MainThread()->Post(this, APP_SEND, new talk_base::TypedMessageData<std::string>(tunnelID));
}

void DataTransferAppImpl::send( const std::string tunnelID)
{
	tunnel_dt_client.SendData(tunnelID);
}

DataTransferAppImpl::~DataTransferAppImpl(){
	delete main_thread;
	delete xmppSocket;
}

void DataTransferAppImpl::OnMessage(talk_base::Message *msg){
	switch (msg->message_id) {
	case APP_LOGIN:{
		talk_base::ScopedMessageData<LoginInfo> *loginInfo = static_cast<talk_base::ScopedMessageData<LoginInfo>*>(msg->pdata);
		login(loginInfo->data()->Username(), loginInfo->data()->Password(), loginInfo->data()->Servername(), loginInfo->data()->ServerPort());
		break;
				   }

	case APP_LOGOUT:{
		logout();
		break;
					}
	case APP_SEND:{
		ASSERT(MainThread()->IsCurrent());
		talk_base::TypedMessageData<std::string> *tunnelIDPtr = static_cast<talk_base::TypedMessageData<std::string>*>(msg->pdata);
		send(tunnelIDPtr->data());
		break;
				  }
	}
}

void DataTransferAppImpl::OnDataReceived(const char* data,  const size_t len, const std::string senderID){
	SignalReceived(data, len, senderID);
}
void DataTransferAppImpl::OnDataSuccessfullySent(const std::string receiverID){
	SignalSent(receiverID);
}
void DataTransferAppImpl::OnDataSendFailed(const std::string errDesc, const ErrorCode errorCode, const std::string receiverID){
	SignalSendFailed(errDesc, receiverID, errorCode);
}

void DataTransferAppImpl::OnDataReceiveFailed(const std::string errDesc, const ErrorCode errorCode, const std::string senderID){
	SignalReceiveFailed(errDesc, senderID, errorCode);
}

void DataTransferAppImpl::OnLoggedIn( const std::string id) {
	SignalLoggedIn(id);
}

void DataTransferAppImpl::OnLoggedOut( const std::string id) {
	SignalLoggedOut(id); 
}
