#include "tunneldatatransferclient.h"

#if defined(_MSC_VER) && (_MSC_VER < 1400)
// The following are necessary to properly link when compiling STL without
// /EHsc, otherwise known as C++ exceptions.
void __cdecl std::_Throw(const std::exception &) {}
std::_Prhand std::_Raise_handler = 0;
#endif

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// TunnelDataTransferClient
///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//-----------------------------------------------------Constructor-----------------------------------------------------------------
TunnelDataTransferClient::TunnelDataTransferClient(buzz::XmppClient *xmppclient, bool isGoogleClient) : 
		xmpp_client_(xmppclient),
		xmpp_server(""),
		is_google_client(isGoogleClient) {
			xmpp_client_.reset(xmppclient);
		}

//-------------------------------------------------------Destructor----------------------------------------------------------------
TunnelDataTransferClient::~TunnelDataTransferClient() {
	CleanAll();
	delete worker_thread_;
}

void TunnelDataTransferClient::CleanAll() {
	CleanStreamTunnelMap(outgoing_st_map);
	CleanStreamTunnelMap(incoming_st_map);

	CleanTunnelStreamMap(outgoing_tunnel_st_map);
	CleanTunnelStreamMap(incoming_tunnel_st_map);

    CleanTunnelCacheMap(outgoing_tunnel_map);
	CleanTunnelCacheMap(incoming_tunnel_map);

	CleanSessionStreamMap(outgoing_session_st_map);
	CleanSessionStreamMap(incoming_session_st_map);
}

void TunnelDataTransferClient::CleanStreamTunnelMap( StreamTunnelMap stream_map ) {
	stream_map.clear();
}

void TunnelDataTransferClient::CleanTunnelStreamMap( TunnelStreamMap tunnel_st_map ) {
	tunnel_st_map.clear();
}

void TunnelDataTransferClient::CleanTunnelCacheMap( TunnelCacheMap cache_map ) {
	TunnelCacheMap::iterator iter;
	while(iter != cache_map.end()){
		TunnelCache* tcache = iter->second;
		delete tcache;
		iter++;
	}

	cache_map.clear();
}

void TunnelDataTransferClient::CleanSessionStreamMap( SessionStreamMap session_st_map ) {
	session_st_map.clear();
}

//--------------------------------------------------------Public methods-------------------------------------------------------------
void TunnelDataTransferClient::OnStateChange(buzz::XmppEngine::State state) {
	switch (state) {
	case buzz::XmppEngine::STATE_START:
		LOG(INFO)<< "Login started..." << std::endl;
		break;
	case buzz::XmppEngine::STATE_OPENING:
		LOG(INFO)<< "Logging in... " << std::endl;
		break;
	case buzz::XmppEngine::STATE_OPEN:
		LOG(INFO)<< "Logged in as: " << xmpp_client_->jid().Str() << std::endl;
		OnSignon();
		SignalLoggedIn(xmpp_client_->jid().node());
		break;
	case buzz::XmppEngine::STATE_CLOSED:
		LOG(INFO)<< "Logged out." << std::endl;
		SignalLoggedOut(xmpp_client_->jid().node());
		exit(0);
	}
}

void TunnelDataTransferClient::AddData(const char* data,
									   const size_t len,
									   const std::string tunnelID) {
	TunnelCacheMap::iterator iter = outgoing_tunnel_map.find(tunnelID);
	if(iter == outgoing_tunnel_map.end()){
		TunnelCache* tunnel_cache = new TunnelCache(tunnelID, true);
		tunnel_cache->AppendDataCache(len, data);
		outgoing_tunnel_map[tunnelID] = tunnel_cache;
	} else{
		iter->second->AppendDataCache(len, data);
	}
}

void TunnelDataTransferClient::SendData( const std::string tunnelID ) {
	TunnelStreamMap::iterator ts_iter = outgoing_tunnel_st_map.find(tunnelID);
	
	//Create a new session if tunnel is not exist. 
	//If the tunnel is exist but it is a zombie tunnel, we would even not care about it. 
	//Since we can fast fail our data transmission and close this zombie tunnel and notify the upper layer.
	if(ts_iter == outgoing_tunnel_st_map.end()){
		//Tunnel id is receiver id.
		buzz::Jid remoteJid = fromNodeID(tunnelID);
		if(remoteJid.Compare(buzz::Jid()) == 0) {
			OnDatasendFailed(tunnelID, MalformedID, "Xmpp server not set");
			return;
		}

		talk_base::StreamInterface* stream = session_client_->CreateTunnel(remoteJid);
		stream->SignalEvent.connect(this, &TunnelDataTransferClient::OnOutgoingStreamEvent);
		outgoing_st_map[stream] = tunnelID;
		outgoing_tunnel_st_map[tunnelID] = stream;
		LOG(INFO)<<"[Outgoing Tunnel "<< tunnelID << "] is created";
	} else {
		//When two continuous data block have a large transfer interval, in case the "write" of the second transfer is not triggered,
		//we trigger the write manually if the channel is idle.
		TunnelCacheMap::iterator tc_iter = outgoing_tunnel_map.find(tunnelID);
		if(tc_iter != outgoing_tunnel_map.end() && tc_iter->second->GetStatus() != Establishing && tc_iter->second->GetStatus() != Closed){
			LOG(INFO) << "[Outgoing Tunnel "<< tunnelID << "] SE_WRITE is forcedly triggered.";
			const_cast<talk_base::StreamInterface *>(ts_iter->second)->SignalEvent(const_cast<talk_base::StreamInterface *>(ts_iter->second), talk_base::SE_WRITE, 0);
		}
	}
}

void TunnelDataTransferClient::OnIncomingTunnel( cricket::TunnelSessionClient* sessionClient,
												 buzz::Jid remoteJid,
												 cricket::Session* session ) {
	//Incoming tunnel id is remote node id.
	const std::string tunnelID = toNodeID(remoteJid);
	
	//Check if there is an old and not closed tunnel first.
	TunnelStreamMap::iterator ts_iter = incoming_tunnel_st_map.find(tunnelID);
	if(ts_iter != incoming_tunnel_st_map.end()){
		LOG(WARNING) <<"[Incoming Tunnel "<< tunnelID << "] the old tunnel exist and needed to be cleaned first.";
		CloseStream(ts_iter->second, true);
	}

	//Accept tunnel and attach event handler.
	talk_base::StreamInterface* stream = sessionClient->AcceptTunnel(session);
	stream->SignalEvent.connect(this, &TunnelDataTransferClient::OnIncomingStreamEvent);

	//Add new incoming tunnel.
	TunnelCache* incomingCache = new TunnelCache(tunnelID, false);
	incoming_tunnel_map[tunnelID] = incomingCache;
	incoming_st_map[stream] = tunnelID;
	incoming_tunnel_st_map[tunnelID] = stream;
	
	LOG(INFO)<<"[Incoming Tunnel "<< tunnelID << "] is created";
}

void TunnelDataTransferClient::OnOutgoingStreamEvent(talk_base::StreamInterface* stream, int events, int err) {
	
	StreamTunnelMap::iterator iter = outgoing_st_map.find(stream);
	if(iter == outgoing_st_map.end()){
		//In some cases, the session has error, then the cache is cleaned for that session and a tunnel close is triggered for that session (Event should be Close(8)).
		LOG(LS_WARNING)<<"[Outgoing Tunnel] stream not found in cache. Event = " << events;
		return;
	}
	const std::string tunnelID = iter->second;
	TunnelCache* tunnel_cache = GetTunnelCacheByID(tunnelID, true);
	if(tunnel_cache == NULL){
		LOG(LERROR) << "[Outgoing Tunnel "<< tunnelID << "] tunnel not found.";
		return;
	}

	//LOG(INFO) <<"[Outgoing Tunnel "<< tunnelID << "] on outgoing event at: "<< talk_base::Time();

	if(events & talk_base::SE_CLOSE){
		tunnel_cache->SetStatus(Closed);
		OnStreamClose(tunnelID, stream, err, false);
		//We should clean the tunnel whenever stream is closed 
		//since close event could triggered by our GC or timeout in session manager.
		CleanOutgoingTunnel(stream);

	} if(events & talk_base::SE_OPEN){
		tunnel_cache->SetStatus(Idle);
	} if(events & talk_base::SE_WRITE){
		if(!outgoing_tunnel_map.empty()) {
			tunnel_cache->SetStatus(Busy);
			ErrorCode errCode;
			if(tunnel_cache != NULL && !WriteToStream(stream, tunnelID, tunnel_cache, errCode)) {
				if(errCode == StreamWriteError){
					OnDatasendFailed(tunnelID, errCode, "Outgoing stream errors");
					CloseStream(stream, false);
				}
			} 
			tunnel_cache->SetStatus(Idle);
		}
	} if(events & talk_base::SE_READ){
	} 
} 

void TunnelDataTransferClient::OnIncomingStreamEvent(talk_base::StreamInterface* stream, int events, int err) {
	//uint32 start = talk_base::Time();
	StreamTunnelMap::iterator iter = incoming_st_map.find(stream);
	ASSERT(iter != incoming_st_map.end());
	const std::string tunnelID = iter->second;

	if(events & talk_base::SE_CLOSE){
		OnStreamClose(tunnelID, stream, err, true);
		CleanIncomingTunnel(stream);
	} if(events & talk_base::SE_OPEN) {
	} if(events & talk_base::SE_WRITE) {
	} if(events & talk_base::SE_READ) {
		if(!incoming_tunnel_map.empty()){
			TunnelCache* tunnel_cache = GetTunnelCacheByID(tunnelID, false);
			ErrorCode errCode;
			if(tunnel_cache != NULL && !ReadFromStream(stream, tunnelID, tunnel_cache, errCode)){
				if(errCode == StreamReadError){
					OnDataReceiveFailed(tunnelID, errCode, "Incoming stream errors");
					CloseStream(stream, true);
				}
			} else if(tunnel_cache == NULL){
				LOG(LERROR) << "[Incoming Tunnel "<< tunnelID << "] cache not found.";
			}
			//uint32 end = talk_base::Time();
			//LOG(INFO) <<"[Incoming Tunnel "<< tunnelID << "] read at: "<<end << ", time cost =" << (end - start);
		}
	}
}

//-------------------------------------------Private methods------------------------------------------------------------------------
void TunnelDataTransferClient::OnJingleInfo(const std::string & relay_token,
	const std::vector<std::string> &relay_addresses,
	const std::vector<talk_base::SocketAddress> &stun_addresses) {
		port_allocator_->SetStunHosts(stun_addresses);
		port_allocator_->SetRelayHosts(relay_addresses);
		port_allocator_->SetRelayToken(relay_token);
}

void TunnelDataTransferClient::OnSignon() {
	port_allocator_.reset(new cricket::HttpPortAllocator(&network_manager_, cricket::CT_TUNNEL));

	worker_thread_ = new talk_base::Thread();
	session_manager_.reset(new cricket::SessionManager(port_allocator_.get(), worker_thread_));
	worker_thread_->Start();

	session_manager_task.reset(new cricket::SessionManagerTask(xmpp_client_.get(), session_manager_.get()));
	session_manager_task->EnableOutgoingMessages();								 
	session_manager_task->Start();

	if(is_google_client){
		buzz::JingleInfoTask *jingle_info_task = new buzz::JingleInfoTask(xmpp_client_.get());
		jingle_info_task->RefreshJingleInfoNow();
		jingle_info_task->SignalJingleInfo.connect(this, &TunnelDataTransferClient::OnJingleInfo);
		jingle_info_task->Start();
	} else{
		LoadJingleInfo();
	}

	session_client_.reset(new cricket::TunnelSessionClient(xmpp_client_->jid(), session_manager_.get(), cricket::NS_TUNNEL));
	session_client_->SignalIncomingTunnel.connect(this, &TunnelDataTransferClient::OnIncomingTunnel);
	session_client_->SignalSessionCreated.connect(this, &TunnelDataTransferClient::OnSessionCreated);
	session_manager_->AddClient(cricket::CT_TUNNEL, session_client_.get());
	//Set session timeout as incoming GC timeout.
	session_manager_->set_session_timeout(INCOMING_GC_TIMEOUT);

	outgoing_tunnel_gc_task.reset(new OutgoingTunnelGCTask(this, OUTGOING_GC_TIMEOUT));
	outgoing_tunnel_gc_task->Start();
	incoming_tunnel_gc_task.reset(new IncomingTunnelGCTask(this, INCOMING_GC_TIMEOUT));
	incoming_tunnel_gc_task->Start();
}

void TunnelDataTransferClient::OnDataSent( talk_base::StreamInterface* stream, TunnelCache* outgoing_cache ) {
	//We must clear the sent data cache.
	outgoing_cache->Pop();

	LOG(INFO) <<"[Outgoing Tunnel "<<outgoing_cache->TunnelID() << "] Data sent to "<<
		outgoing_cache->TunnelID() << " at: "<< talk_base::Time(); 

	//We might have more than one data cache in tunnel.
	if(outgoing_cache->Size() != 0){
		stream->SignalEvent(stream, talk_base::SE_WRITE, 0);
	}

	//Notify sent on another thread.
	SignalDataSuccessfullySent(outgoing_cache->TunnelID());
}

void TunnelDataTransferClient::OnDatasendFailed( const std::string tunnelID, const ErrorCode err, const std::string errDesc )
{
		SignalDataSentFailed(errDesc, err, tunnelID);
}

void TunnelDataTransferClient::OnDataReceiveFailed( const std::string tunnelID, const ErrorCode err, const std::string errDesc )
{
		//Remove the incoming tunnel first.
		GetTunnelCacheByID(tunnelID, false)->Clear();
		//Then notify client the failure.
		SignalDataReceiveFailed(errDesc, err, tunnelID);
}

void TunnelDataTransferClient::OnDataReceived( char* data, const size_t len, const std::string tunnelID )
{
	SignalDataReceived(data, len, tunnelID);
}

void TunnelDataTransferClient::ResetJingleInfo(const JingleInfo jingleInfo)
{
	jingle_info = jingleInfo;
	if(port_allocator_.get() != NULL){
		LoadJingleInfo();
	}
}

void TunnelDataTransferClient::LoadJingleInfo(){
	std::vector<std::string> relay_addresses;
	std::vector<talk_base::SocketAddress> stun_addresses;

	relay_addresses.push_back(jingle_info.RelayAddr());
	stun_addresses.push_back(talk_base::SocketAddress(jingle_info.StunAddr(), jingle_info.StunPort()));

	LOG(INFO) <<"STUN server set to: "<<jingle_info.StunAddr()<<":"<<jingle_info.StunPort()
		<<" Relay server set to: "<<jingle_info.RelayAddr()<<":"<<jingle_info.RelayPort();

	port_allocator_->SetStunHosts(stun_addresses);
	port_allocator_->SetRelayHosts(relay_addresses);
	port_allocator_->SetRelayToken(DEFAULT_RELAY_TOKEN);
}

bool TunnelDataTransferClient::WriteToStream( talk_base::StreamInterface* stream,
											  const std::string tunnelID,
											  TunnelCache* outgoing_cache,
											  ErrorCode& errCode ) {
    int error;
	size_t written;
	if(outgoing_cache->Size() == 0) {
		LOG(LERROR) << "No data in outgoing data cache of tunnel: " << tunnelID;
		return false;
	}
	DataCache* data_cache = outgoing_cache->Front();
	size_t& remaining_len = data_cache->RemainingLen();

	//Write to the stream until write blocked (waiting for another write event) 
	//or write error (notify data send failed)
	//or write EOS (should not happened).
	while(remaining_len > 0) {
		//uint32 start = talk_base::Time();
		talk_base::StreamResult result = stream->Write(data_cache->RemainingData(), remaining_len, &written, &error);
		//uint32 end = talk_base::Time();
		//LOG(INFO) <<"[Outgoing Tunnel "<< tunnelID << "] write to stream at: "<<end << ", time cost =" << (end - start);
		if( result == talk_base::SR_SUCCESS ) {
			remaining_len -= written;
			continue;
		} else if(result == talk_base::SR_ERROR){
			LOG(LERROR)<<"[Outgoing Tunnel "<<tunnelID << "] stream error on write. error code = "<<error;
			errCode = StreamWriteError;
			return false;
		} else if(result == talk_base::SR_BLOCK){
			//LOG(WARNNING)<<"[Outgoing Tunnel "<<tunnelID << "] stream write blocked at:" << talk_base::Time();
			return false;
		} else if(result == talk_base::SR_EOS){
			LOG(LERROR)<<"[Outgoing Tunnel "<<tunnelID << "] write end of stream.";
			return false;
		}
	}

	//We will be here only if current write finished.
	ASSERT(remaining_len == 0);
	//Notify sent on client thread.
	OnDataSent(stream, outgoing_cache);
	return true;
}

bool TunnelDataTransferClient::ReadFromStream( talk_base::StreamInterface* stream,
											   const std::string tunnelID,
											   TunnelCache* incoming_cache,
											   ErrorCode& errCode ) {
	const size_t max_read = READ_BUF_SIZE - 1;
	size_t count;
	int error;

	//Read max_read length of data from stream at each time. And process read data,
	//Or: blocked (waiting for another read)
	//    error (notify read failed)
	//    EOS (waiting for another read)
	memset(read_buf, 0, sizeof(read_buf));
	talk_base::StreamResult result = stream->Read(read_buf, max_read, &count, &error);
	
	if(result == talk_base::SR_SUCCESS){
		size_t process_start = 0;
		ProcessIncomingData(process_start, count, incoming_cache);
	} else if(result == talk_base::SR_BLOCK){
		//LOG(WARNING)<<"[Incoming Tunnel "<<tunnelID << "] stream read blocked at:" << talk_base::Time();
		return false;
	} else if(result == talk_base::SR_ERROR){
		LOG(LERROR)<<"[Incoming Tunnel "<<tunnelID <<"] stream error on read, error=" << error;
		errCode = StreamReadError;
		return false;
	} else if(result == talk_base::SR_EOS){
		LOG(WARNING)<<"[Incoming Tunnel "<<tunnelID <<"] read end of stream."; 
		return false;
	}

	return true;
}

/************************************************************************/
/*Process incoming data either add to cache or dispatch to listeners*****/
//offset:             process start point in data buf.
//count:              to be processed data count.
//incoming_cache:     the incoming tunnel cache.
/************************************************************************/
void TunnelDataTransferClient::ProcessIncomingData(size_t& offset,
												   const size_t count,
												   TunnelCache* incoming_cache) {
	if(count == 0){ 
		return;
	}

	DataCache* data_cache = incoming_cache->GetAvaliableIncomingDataCache();

	if(!data_cache->IncomingDataLenInfo().IsLenInfoCompleted()) {
		ProcessLen(incoming_cache->TunnelID(), offset, count, data_cache);
	} else{
		ProcessData(incoming_cache->TunnelID(), offset, count, data_cache);
	}
}

//Process length info from incoming data.
void TunnelDataTransferClient::ProcessLen( const std::string tunnelID,
										   size_t& offset,
										   const size_t count,
										   DataCache* data_cache ) {

	int incompleted_leninfo_count = data_cache->IncomingDataLenInfo().GetIncompletedCount();
	int processed; 
	if(incompleted_leninfo_count > count){
		processed = count;
	} else {
		processed = incompleted_leninfo_count;
	}

	data_cache->IncomingDataLenInfo().AddCurrentLenInfo(read_buf, offset, offset + processed);
	offset += processed;
	if(data_cache->IncomingDataLenInfo().IsLenInfoCompleted()){
		//IncompletedData means completed len info.
		data_cache->SetFlag(IncompletedData);
		data_cache->InitCacheSize();
		ProcessData(tunnelID, offset, count - processed, data_cache);
	} else{
		//LenInfo is not completed means "incompleted_leninfo_count > count", so count == processed,
		//we will actually finish the incoming data process.
		ASSERT(processed == count);
		ProcessIncomingData(offset, count - processed, GetTunnelCacheByID(tunnelID, false));
	}
}

//Process data with data length already known.
void TunnelDataTransferClient::ProcessData( const std::string tunnelID,
											size_t& offset,
											const size_t count,
											DataCache* data_cache ) {

	size_t remaining_data_len = data_cache->RemainingLen();
	if(remaining_data_len <= count) {
		//Complete the current data block first and continue process for remaining data if any.
		data_cache->AddData(read_buf, offset, remaining_data_len);
		data_cache->AddTerminator();
		data_cache->SetFlag(CompletedData);
		OnReceivedCompleteDataBlock(tunnelID, data_cache);

		offset += remaining_data_len;
		ProcessIncomingData(offset, count - remaining_data_len, GetTunnelCacheByID(tunnelID, false));
	} else {
		//This is not a completed reading yet, just append data into data cache.
		data_cache->AddData(read_buf, offset, count);
		offset += count;
		ProcessIncomingData(offset, 0, GetTunnelCacheByID(tunnelID, false));
	}
}

void TunnelDataTransferClient::OnReceivedCompleteDataBlock( const std::string tunnelID,
														    DataCache* data_cache ) {
	talk_base::CritScope lock(&cs_);
	
	//Notify client the new data.
	OnDataReceived(data_cache->Data(), data_cache->Size(), tunnelID);
	
	LOG(INFO) <<"[Incoming Tunnel "<< tunnelID <<"] received a completed data," << " len = " << data_cache->Size() <<" at: "<< talk_base::Time();
	
	//Then remove the completed data cache.
	GetTunnelCacheByID(tunnelID, false)->RemoveCompletedIncomingCaches();
}

void TunnelDataTransferClient::CloseStream( const talk_base::StreamInterface* stream, bool isIncoming ) {
	const_cast<talk_base::StreamInterface*>(stream)->Close();
	if(isIncoming){
		CleanIncomingTunnel(stream);
	} else{
		CleanOutgoingTunnel(stream);
	}
}

void TunnelDataTransferClient::OnTunnelClosedWithErrors(const talk_base::StreamInterface* stream, int err) {
	std::string errDescription = "Data tunnel closed on errors";

	if(outgoing_st_map.find(stream) != outgoing_st_map.end()){
		StreamTunnelMap::iterator iter_write = outgoing_st_map.find(stream);
		OnDatasendFailed(iter_write->second, NotifiedTunnelErrors, errDescription);
	} else if(incoming_st_map.find(stream) != incoming_st_map.end()){
		StreamTunnelMap::iterator iter_read = incoming_st_map.find(stream);
		OnDataReceiveFailed(iter_read->second, NotifiedTunnelErrors, errDescription);
	} else{
		//Shouldn't come here.
		ASSERT(true);
	}
}

void TunnelDataTransferClient::OnStreamClose( const std::string tunnelID, talk_base::StreamInterface* stream, int err, bool is_incoming ) {
	std::string dbg_descriptor = is_incoming ? "Incoming" : "Outgoing";
	if(err == 0){
		LOG(INFO) << "["<<dbg_descriptor << " Tunnel " << tunnelID << "] closed ";
	} else if(err != -1){
		//'-1' means the tunnel is terminated by session, we have already handled session error, so it is no need to handle the error again here.
		LOG(LERROR) << "["<<dbg_descriptor << " Tunnel " << tunnelID<<"] closed with error: "<< err;
		OnTunnelClosedWithErrors(stream, err);
	}
}

void TunnelDataTransferClient::CleanOutgoingTunnel(const talk_base::StreamInterface* stream){
	talk_base::CritScope lock(&cs_);
	StreamTunnelMap::iterator st_iter = outgoing_st_map.find(stream);

	if(st_iter != outgoing_st_map.end()){
		std::string tunnelID = st_iter->second;
		
		//Remove StreamTunnelMap
		outgoing_st_map.erase(st_iter);
		
		//Remove TunnelStreamMap
		TunnelStreamMap::iterator ts_iter = outgoing_tunnel_st_map.find(tunnelID);
		if(ts_iter != outgoing_tunnel_st_map.end()){
			outgoing_tunnel_st_map.erase(ts_iter);
		}

		//Remove TunnelCacheMap
		TunnelCacheMap::iterator tc_iter = outgoing_tunnel_map.find(tunnelID);
		if(tc_iter != outgoing_tunnel_map.end()){
			delete tc_iter->second;
			outgoing_tunnel_map.erase(tc_iter);
		}

		//Remove SessionStreamMap
		SessionStreamMap::iterator s_st_iter = outgoing_session_st_map.find(tunnelID);
		if(s_st_iter != outgoing_session_st_map.end()){
			outgoing_session_st_map.erase(s_st_iter);
		}
	}
}

void TunnelDataTransferClient::CleanIncomingTunnel( const talk_base::StreamInterface* stream ) {
	talk_base::CritScope lock(&cs_);
	StreamTunnelMap::iterator st_iter = incoming_st_map.find(stream);

	if(st_iter != incoming_st_map.end()){
		std::string tunnelID = st_iter->second;

		//Remove StreamTunnelMap
		incoming_st_map.erase(st_iter);

		//Remove TunnelStreamMap
		TunnelStreamMap::iterator ts_iter = incoming_tunnel_st_map.find(tunnelID);
		if(ts_iter != incoming_tunnel_st_map.end()){
			incoming_tunnel_st_map.erase(ts_iter);
		}

		//Remove TunnelCacheMap
		TunnelCacheMap::iterator tc_iter = incoming_tunnel_map.find(tunnelID);
		if(tc_iter != incoming_tunnel_map.end()){
			delete tc_iter->second;
			incoming_tunnel_map.erase(tc_iter);
		}

		//Remove SessionStreamMap
		SessionStreamMap::iterator s_st_iter = incoming_session_st_map.find(tunnelID);
		if(s_st_iter != incoming_session_st_map.end()){
			incoming_session_st_map.erase(s_st_iter);
		}
	}
}

TunnelCache* TunnelDataTransferClient::GetTunnelCacheByID(const std::string tunnelID, const bool is_outgoing){
	if(is_outgoing && outgoing_tunnel_map.empty() || !is_outgoing && incoming_tunnel_map.empty()){
		return NULL;
	}
	TunnelCacheMap::iterator iter = is_outgoing ? outgoing_tunnel_map.find(tunnelID) : incoming_tunnel_map.find(tunnelID);

	if((is_outgoing && iter != outgoing_tunnel_map.end()) || (!is_outgoing && iter != incoming_tunnel_map.end())){
		return iter->second;
	}
	return NULL;
}

void TunnelDataTransferClient::DoOutgoingTunnelGC() {
	DoTunnelGC(outgoing_tunnel_map, outgoing_tunnel_st_map, true);
}

void TunnelDataTransferClient::DoIncomingTunnelGC() {
	DoTunnelGC(incoming_tunnel_map, incoming_tunnel_st_map, false);
}


void TunnelDataTransferClient::DoTunnelGC( TunnelCacheMap& tunnel_cache_map,  TunnelStreamMap& tunnel_st_map, bool isOutgoing ) {
	TunnelCacheMap::iterator iter = tunnel_cache_map.begin();
	while(iter != tunnel_cache_map.end()) {
		TunnelCache* tunnel_cache = iter->second;
		const std::string tunnelID = iter->first;
		//In case the tunnel cache is GC-ed, so the iterator will at a bad position, we increase iterator before doing GC.
		iter++;
		
		#if 0
		LOG(INFO) << (isOutgoing ?  "[Outgoing" : "[Incoming") << " Tunnel-GC] Tunnel " << tunnelID << " size = " << tunnel_cache->Size();
		#endif
		
		if(tunnel_cache->ShouldPrepareTunnelGC()){
			tunnel_cache->ScheduleToNextGC();
		} else if(tunnel_cache->ShouldDoTunnelGC()) {
			talk_base::CritScope lock(&cs_);
			TunnelStreamMap::iterator ts_iter = tunnel_st_map.find(tunnelID);
			if(ts_iter != tunnel_st_map.end() && ts_iter->second != NULL) {
				LOG(INFO) << (isOutgoing ?  "[Outgoing" : "[Incoming") << " Tunnel-GC] Tunnel " << tunnelID << " is GC-ed.";
				CloseStream(ts_iter->second, !isOutgoing);
			}
		} else {
			tunnel_cache->ResetTunnelGC();
		}
	}
}

void TunnelDataTransferClient::OnSessionCreated(cricket::Session* session, talk_base::StreamInterface* stream, bool is_incoming ) {
	session->SignalError.connect(this, &TunnelDataTransferClient::OnSessionError);
	if(is_incoming) {
	  LOG_F(LS_INFO)<<"Add incoming session" << session->id()<<" to cache.";
	  incoming_session_st_map[session->id()] = stream;
	} else{
	  outgoing_session_st_map[session->id()] = stream;	
	}
}

void TunnelDataTransferClient::OnSessionError( cricket::BaseSession* session, cricket::BaseSession::Error error ) {
	const std::string sid = session->id();
	
	SessionStreamMap::iterator s_st_iter = outgoing_session_st_map.find(sid);

	if(s_st_iter != outgoing_session_st_map.end()) {
		StreamTunnelMap::iterator st_t_iter = outgoing_st_map.find(s_st_iter->second);
		if(st_t_iter != outgoing_st_map.end()) {
			TunnelCache* tc = GetTunnelCacheByID(st_t_iter->second, true);
			if(tc != NULL) {
				if(error == cricket::BaseSession::Error::ERROR_TIME) {
					//For outgoing session timeout, it will not close tunnel stream, so we should do it by ourself. And we should notify a failed send.
					LOG(LERROR) <<"[Outgoing tunnel "<<st_t_iter->second<<" ] session time out.";
					OnDatasendFailed(tc->TunnelID(), SessionTimeOut, "Session time-out");
					CloseStream(s_st_iter->second, false);
				} else if(error == cricket::BaseSession::Error::ERROR_RESPONSE) {
					//We should notify user when remote receiver is not available(offline).
					LOG(LERROR) <<"[Outgoing tunnel "<<st_t_iter->second<<" ] remote node is not online.";
					OnDatasendFailed(tc->TunnelID(), ReceiverUnavailable, "Receiver is not online");
					CleanOutgoingTunnel(s_st_iter->second);
				} else if(error != cricket::BaseSession::Error::ERROR_NONE){
					LOG(LERROR) <<"[Outgoing tunnel "<<st_t_iter->second<<" ] Errors on session.";
					OnDatasendFailed(tc->TunnelID(), SessionErrors, "Session errors");
					CleanOutgoingTunnel(s_st_iter->second);
				}
			} else{
				LOG(WARNING) <<"[Outgoing tunnel "<<st_t_iter->second<<" ] on session error, and no tunnel cache found for the tunnel.";
			}
		} else{
			LOG(WARNING) <<"On session error, and no outgoing stream found for session " << sid;
		}
	} else{
		s_st_iter = incoming_session_st_map.find(sid);
		if(s_st_iter != incoming_session_st_map.end()){
			LOG(LERROR) <<"[Incoming tunnel "<<s_st_iter->second<<" ] Errors on session. error = " << error;
		}
	}
}

buzz::Jid TunnelDataTransferClient::fromNodeID(const std::string id) {
	if(xmpp_server.compare("") == 0){
		LOG(LERROR) << "Xmpp server not initialized.";
		return buzz::Jid();
	}

	buzz::Jid jid(id, xmpp_server, JID_RESOURCE);
	return jid;
}

std::string TunnelDataTransferClient::toNodeID( const buzz::Jid jid ) { 
	return jid.node();
}

void TunnelDataTransferClient::SetXMPPServer( const std::string xmpp_server_name ) {
	xmpp_server = xmpp_server_name;
}