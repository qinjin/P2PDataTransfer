#include"tunnelcache.h"

///////////////////////////////////////////////////////////////////////////////
// LenInfo
///////////////////////////////////////////////////////////////////////////////

//Get length info.
int LenInfo::DataLen() {
	ASSERT(IsLenInfoCompleted());
	for(int i=0; i<LENINFO_SIZE; i++){
		return ( (len_info[0]& 0xff)  |
			(len_info[1] & 0xff) <<8  |
			(len_info[2] & 0xff) <<16 |
			(len_info[3] & 0xff) <<24);
	}
}

//Get length info from start to end and add as length info.  
void LenInfo::AddCurrentLenInfo( const char* buf, int start, int end ) {
	ASSERT(incompleted_info_count >= end - start);
	for(int i = start; i<end; i++){
		if(incompleted_info_count > 0){
			len_info[LENINFO_SIZE - incompleted_info_count] = buf[i];
			incompleted_info_count--;
		}
	}

	if(incompleted_info_count == 0){
		is_info_complete = true;
	}
}

//Reset length info after data dispatched.
void LenInfo::Reset() {
	is_info_complete = false;
	incompleted_info_count = LENINFO_SIZE;
	for(int i=0; i<LENINFO_SIZE; i++){
		len_info[i] = 0;
	}
}

///////////////////////////////////////////////////////////////////////////////
// DataCache
///////////////////////////////////////////////////////////////////////////////

DataCache::DataCache( const bool isOutgoing = false) : 
					cache_size(-1),
					remaining_len(-1),
					is_outgoing(isOutgoing),
					incoming_data_flag(IncompletedLenInfo) {
	data.reserve(1024 * 32);
}

DataCache::DataCache(const char* buf, const size_t len, const bool isOutgoing = true) :
					cache_size(len + 4),
					remaining_len(len + 4),
					is_outgoing(isOutgoing),
					incoming_data_flag(Invalid) {
	data.reserve(len + 4);
	BuildOutgoingData(buf, len);
}

DataCache::~DataCache() {}

const size_t DataCache::Size() {
	return cache_size;
}

char* DataCache::Data() {
	return &data[0];
}

//Outgoing cache method
void DataCache::BuildOutgoingData(const char* buf, const size_t len ) {
	//Add length info as header.
	data.push_back(len & 0xff);
	data.push_back((len >> 8) & 0xff);
	data.push_back((len >> 16) & 0xff);
	data.push_back((len >> 24) & 0xff);

	data.insert(data.end(), buf, buf+len);
}

//Outgoing cache method
char* DataCache::RemainingData() {
	ASSERT(is_outgoing);
	return &data[(Size() - remaining_len)];
}

//Incoming cache method 
void DataCache::AddData( const char* buf, size_t offset, size_t len ) {
	ASSERT(!is_outgoing);
    ASSERT(len <= remaining_len);
	
	//Add data from offset of buf to current data cache with length is len.
	data.insert(data.end(), buf+offset, buf+offset+len);
	remaining_len -= len;
}

//Incoming cache method
void DataCache::AddTerminator() {
	data.push_back(0);
}

//Incoming cache method
void DataCache::InitCacheSize() {
	cache_size = len_info.DataLen();
	remaining_len = cache_size;
}

///////////////////////////////////////////////////////////////////////////////
// TunnelCache
///////////////////////////////////////////////////////////////////////////////

TunnelCache::TunnelCache(const std::string tunnelID, bool isOutgoing) : 
				tunnel_id(tunnelID),
				is_outgoing(isOutgoing),
				should_gc(false),
				status(Establishing),
				tunnel_empty_start(talk_base::Time()) {}

TunnelCache::~TunnelCache() {
	while(Size() > 0){
		Pop();
	}
}

const std::string TunnelCache::TunnelID() {
	return tunnel_id;
}

//Get the first data cache.
DataCache* TunnelCache::Front() { 
	talk_base::CritScope lock(&cs_);
	return data_cache_queue.front();
}

//Get the last data cache.
DataCache* TunnelCache::Back() { 
	talk_base::CritScope lock(&cs_);
	return data_cache_queue.back();
}

//Pop 
void TunnelCache::Pop() {
	talk_base::CritScope lock(&cs_);
	//Pop will call data cache destructor.
	DataCache* dc = data_cache_queue.front();
	data_cache_queue.pop();
	delete dc;

	if(Size() == 0) {
		tunnel_empty_start = talk_base::Time();
	}
}

//Get the number of data cache in the tunnel.
const size_t TunnelCache::Size() { 
	talk_base::CritScope lock(&cs_);
	return data_cache_queue.size(); 
}

//If tunnel cache status is active. 
const bool TunnelCache::IsActive() {
	return Size() != 0;
}

void TunnelCache::SetStatus( TunnelStatus newStatus ) {
	talk_base::CritScope lock(&cs_);
	status = newStatus;
}

TunnelStatus TunnelCache::GetStatus() {
	return status;
}

//Clear tunnel cache: remove all data cache in that tunnel.
void TunnelCache::Clear() {
	talk_base::CritScope lock(&cs_);
	while(Size() > 0){
		Pop();
	}
}

//Schedule to next GC to close the tunnel.
//This could be aborted if tunnel is active in next GC.
void TunnelCache::ScheduleToNextGC() {
	talk_base::CritScope lock(&cs_);
	should_gc = true;
	LOG(INFO) << "Schedule to next GC.";
}

bool TunnelCache::ShouldDoTunnelGC() {
	talk_base::CritScope lock(&cs_);
	
	//Check activity as last resort before GC-ed.
	if(IsActive()){
		LOG(INFO) << "Reset tunnel GC in last check before its GC-ed.";
		ResetTunnelGC();
	}

	return should_gc;
}

//Prepare the tunnel GC only if tunnel is currently inactive and it is inactive more than GC Timeout period.
bool TunnelCache::ShouldPrepareTunnelGC() {
	talk_base::CritScope lock(&cs_);
	
	if(!should_gc && 
		!IsActive() && 
		(talk_base::Time() - tunnel_empty_start) >= (is_outgoing ? OUTGOING_GC_TIMEOUT : INCOMING_GC_TIMEOUT)) {
			return true;
	}

	return false;
}

void TunnelCache::ResetTunnelGC() {
	talk_base::CritScope lock(&cs_);
	
	should_gc = false;
}

//Outgoing tunnel method.
//Append outgoing data to cache.
void TunnelCache::AppendDataCache(const size_t len, const char* data) {
	talk_base::CritScope lock(&cs_);

	//uint32 start = talk_base::Time();
	DataCache* data_cache = new DataCache(data, len);
	data_cache_queue.push(data_cache);
	//LOG(INFO) << "[Tunnel " << TunnelID()  << ".AppendDataCache] time cost = " << talk_base::Time() - start;
}

//Incoming tunnel method.
DataCache* TunnelCache::GetAvaliableIncomingDataCache() {
	talk_base::CritScope lock(&cs_);
	
	if(Size() == 0 || Back()->GetFlag() == CompletedData){
		DataCache* dc = new DataCache();
		data_cache_queue.push(dc);
		return dc;
	} else {
		return data_cache_queue.back();
	}
}

//Incoming tunnel method.
void TunnelCache::RemoveCompletedIncomingCaches() {
	talk_base::CritScope lock(&cs_);

	if(Size() != 0 && Front()->GetFlag() == CompletedData) {
		Pop();
	}
}