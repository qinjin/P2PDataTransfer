#ifndef TUNNEL_CACHE_H_
#define TUNNEL_CACHE_H_

#include <iomanip>
#include <time.h>

#include "talk/base/logging.h"
#include "talk/base/physicalsocketserver.h"
#include "talk/base/ssladapter.h"
#include "talk/base/sigslot.h"
#include "talk/base/stream.h"
#include "talk/base/criticalsection.h"
#include "talk/p2p/base/session.h"
#include "talk/p2p/base/transportchannel.h"
#include "talk/p2p/base/sessionclient.h"
#include "talk/p2p/client/httpportallocator.h"
#include "talk/p2p/client/sessionmanagertask.h"
#include "talk/xmpp/xmppclientsettings.h"
#include "talk/xmpp/jid.h"
#include "talk/examples/login/xmppthread.h"
#include "talk/examples/login/xmppauth.h"
#include "talk/examples/login/jingleinfotask.h"
#include "talk/examples/login/presenceouttask.h"
#include "talk/examples/login/presencepushtask.h"
#include "talk/session/tunnel/tunnelsessionclient.h"

#include "constants.h"

const int LENINFO_SIZE = 4;

///////////////////////////////////////////////////////////////////////////////
// IncomingDataFlag : incompletedLenInfo -> InCompletedData -> CompletedData
// For outgoing data, the flag is Invalid
///////////////////////////////////////////////////////////////////////////////
enum IncomingDataFlag{
	Invalid,
	IncompletedLenInfo,
	IncompletedData,
	CompletedData
};

/////////////////////////////////////////////////////////////////////////////////////////
// LenInfo: current data length info in the tunnel stream.
/////////////////////////////////////////////////////////////////////////////////////////
class LenInfo{
public:
	LenInfo(){ Reset(); }
	virtual ~LenInfo() {}
	int DataLen();
	void AddCurrentLenInfo(const char* buf, int start_point, int end_point);
	bool IsLenInfoCompleted() { return is_info_complete; }
	int GetIncompletedCount() { return incompleted_info_count; }
	void Reset();
private:
	int incompleted_info_count;
	bool is_info_complete;
	char len_info[LENINFO_SIZE];
};

/////////////////////////////////////////////////////////////////////////////////////////
// DataCache: an unique data transfered between two peers. Could be outgoing or incoming
/////////////////////////////////////////////////////////////////////////////////////////

/****************************************************************/
/******************The data cache structure *********************/
//0             1               2               3
//0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//                           length
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//                           data
//                           ....
//+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/****************************************************************/
class DataCache{
public:
	DataCache( const bool isOutgoing );
	DataCache( const char* buf, const size_t len, const bool isOutgoing );
	virtual ~DataCache();

	//Common methods.
	const size_t Size();
	char* Data();
	size_t& RemainingLen() {return remaining_len;}

	//Outgoing data cache methods
	void BuildOutgoingData(const char* buf, const size_t len );
	char* RemainingData();

	//Incoming data cache methods
	void InitCacheSize();
	void AddData(const char* buf, size_t offset, size_t len);
	void AddTerminator();
	LenInfo& IncomingDataLenInfo() {return len_info;}
	void SetFlag(IncomingDataFlag flag) { incoming_data_flag = flag; }
	IncomingDataFlag GetFlag() {return incoming_data_flag; }

private:
	const bool is_outgoing;
	size_t cache_size;
	std::vector<char> data;
	//For outgoing data, this is the remaining len need to write to the stream.
	//For incoming data, this is the remaining len need to read from the stream.
	size_t remaining_len;
	//For incoming data, we record len info we read each time.
	LenInfo len_info;
	IncomingDataFlag incoming_data_flag;
};

///////////////////////////////////////////////////////////////////////////////
//Tunnel Status: establishing->idle(opened)->busy->idle->PrepareClose->closed
///////////////////////////////////////////////////////////////////////////////
enum TunnelStatus{
	Establishing,
	Idle,
	Busy,
	PrepareClose,
	Closed
};

/////////////////////////////////////////////////////////////////////////////////////////
// TunnelCache: Each tunnel will have one cache.
/////////////////////////////////////////////////////////////////////////////////////////
class TunnelCache {
public:
	TunnelCache(const std::string tunnelID, const bool isOutgoing);
	virtual ~TunnelCache();
	
	const std::string TunnelID();

	DataCache* Front();

	DataCache* Back();

	void Pop();

	const size_t Size();

	const bool IsActive();

	void ScheduleToNextGC();

	bool ShouldDoTunnelGC();

	bool ShouldPrepareTunnelGC();

	void ResetTunnelGC();

	void Clear();

	void SetStatus(TunnelStatus status);

	TunnelStatus GetStatus();

	//Outgoing tunnel methods.
	void AppendDataCache(const size_t len, const char* data);

	//Incoming tunnel methods.
	DataCache* GetAvaliableIncomingDataCache();
	void RemoveCompletedIncomingCaches();
	
private:
	const std::string tunnel_id;
	std::queue<DataCache*> data_cache_queue;
	mutable talk_base::CriticalSection cs_;
	const bool is_outgoing;
	bool should_gc;
	TunnelStatus status;
	uint32 tunnel_empty_start;
};

#endif