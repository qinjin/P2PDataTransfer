#include"datatransferappproxy.h"

DataTransferAppProxy::DataTransferAppProxy() : data_transfer_app(new DataTransferAppImpl()), receiver(0){
	data_transfer_app->SignalLoginFailed.connect(this, &DataTransferAppProxy::OnLoginFailed);
	data_transfer_app->SignalReceived.connect(this, &DataTransferAppProxy::OnReceived);
	data_transfer_app->SignalLoggedIn.connect(this, &DataTransferAppProxy::OnLoggedIn);
	data_transfer_app->SignalLoggedOut.connect(this, &DataTransferAppProxy::OnLoggedOut);
	data_transfer_app->SignalReceiveFailed.connect(this, &DataTransferAppProxy::OnReceiveFailed);
	data_transfer_app->SignalSent.connect(this, &DataTransferAppProxy::OnSent);
	data_transfer_app->SignalSendFailed.connect(this, &DataTransferAppProxy::OnSendFailed);
}

DataTransferAppProxy::~DataTransferAppProxy(){
	deleteReceiver();
	delete data_transfer_app;
}
void DataTransferAppProxy::login(const std::string nodeID, const std::string password, const std::string xmpp_server_name, const int port, const int time_out){
	xmpp_server = xmpp_server_name;
	data_transfer_app->login(nodeID, password, xmpp_server_name, port);
}
void DataTransferAppProxy::logout(){
	data_transfer_app->logout();
}

void DataTransferAppProxy::send(const char* data, const size_t len, const std::string remoteNodeID ){
	//uint32 start = talk_base::Time();
	//Validate first.
	if(!Validate(remoteNodeID)) return;

	//Append outgoing data to corresponded tunnel cache.
	data_transfer_app->AddData(data, len, createTunnelID(remoteNodeID));
	//uint32 end = talk_base::Time();
	//LOG(INFO)<<"[Outgoing Tunnel "<< remoteNodeID << "] DataTransferAppProxy.Send() at: "<< end <<" time cost = "<< (end - start) ;
}

void DataTransferAppProxy::setDebug(const bool isDebug){
	data_transfer_app->SetDebugMode(isDebug);
}

void DataTransferAppProxy::setStunAndRelayInfo( const std::string stunAddr, const int stunPort, const std::string relayAddr, const int relayPort ) {
	data_transfer_app->ResetJingleInfo(JingleInfo(stunAddr, stunPort, relayAddr, relayPort));
}

void DataTransferAppProxy::OnLoginFailed(const std::string id, const std::string errDesc, const ErrorCode errCode){
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onLoginFailed(id, errCode, errDesc);
}

void DataTransferAppProxy::OnReceived(const char* data,const size_t len, const std::string senderID){
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onDataReceived(data, len, senderID);
}

void DataTransferAppProxy::OnReceiveFailed(const std::string errDesc, const std::string senderID, const ErrorCode errCode){
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onDataReceiveFailed(senderID, errCode, errDesc);
}

void DataTransferAppProxy::OnSent(const std::string receiverID) {
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onDataSent(receiverID);
}

void DataTransferAppProxy::OnSendFailed(const std::string errDesc, const std::string receiverID, const ErrorCode errCode) {
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onDataSentFailed(receiverID, errCode, errDesc);
}

void DataTransferAppProxy::OnLoggedIn(const std::string id){
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onLoggedIn(id);
}

void DataTransferAppProxy::OnLoggedOut(const std::string id){
	if(!isReceiverInitialized()){
		return;
	}

	receiver->onLoggedOut(id);
}

bool DataTransferAppProxy::Validate(const std::string id) {
	std::string errDesc;
	if(data_transfer_app->OwnID().compare(id) == 0){
		errDesc = "Can not send data to yourself.";
		LOG(LERROR)<<errDesc;
		OnSendFailed(errDesc, id, InvalidReceiver);
		return false;
	}

	if( id.empty() ){
		errDesc = "Invalid receiver id.";
		LOG(LERROR)<<errDesc;
		OnSendFailed(errDesc, id, MalformedID);
		return false;
	}

	return true;
}

const std::string DataTransferAppProxy::createTunnelID( const std::string remoteNodeID) {
	//For outgoing tunnel, the id is remote node id.
	return remoteNodeID;
}