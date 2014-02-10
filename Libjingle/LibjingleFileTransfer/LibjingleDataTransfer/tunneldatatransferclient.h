#ifndef TUNNEL_DATATRANSFER_CLIENT_H__
#define TUNNEL_DATATRANSFER_CLIENT_H__

#include "tunnelcache.h"
#include "tunnelgctask.h"

const int READ_BUF_SIZE = 1024 * 60;

class OutgoingTunnelGCTask;
class IncomingTunnelGCTask;

///////////////////////////////////////////////////////////////////////////////
// ErrorCode
///////////////////////////////////////////////////////////////////////////////
enum ErrorCode{
	//Session errors.
	SessionTimeOut,
	SessionErrors,

	//Stream errors.
	StreamReadError,
	StreamWriteError,

	//Validation errors.
	InvalidReceiver,
	MalformedID,

	//Status errors.
	ReceiverUnavailable,

	//Tunnel errors,
	NotifiedTunnelErrors,

	//Xmpp server connection timeout
	LoginTimeout
};

///////////////////////////////////////////////////////////////////////////////
// Sending and receiving thread messages
///////////////////////////////////////////////////////////////////////////////
enum DataThreadMessage{
	DATA_SENT,
	DATA_RECEIVED,
	DATA_SEND_ERROR,
	DATA_RECV_ERROR
};

///////////////////////////////////////////////////////////////////////////////
// JingleInfo
///////////////////////////////////////////////////////////////////////////////
class JingleInfo{
public:
	JingleInfo() : 
		    stun_addr(DEFAULT_STUN_ADDR), 
			stun_port(DEFAULT_STUN_PORT),
			relay_addr(DEFAULT_RELAY_ADDR), 
			relay_port(DEFAULT_RELAY_PORT) {}
	JingleInfo(const std::string stunAddr, const int stunPort, const std::string relayAddr, const int relayPort) :
			stun_addr(stunAddr),
			stun_port(stunPort), 
			relay_addr(relayAddr),
			relay_port(relayPort) {}
	const std::string StunAddr() { return stun_addr; }
	const int StunPort() { return stun_port; }
	const std::string RelayAddr() { return relay_addr; }
	const int RelayPort() { return relay_port; }
private:
	std::string stun_addr;
	int stun_port;
	std::string relay_addr;
	int relay_port;
};

///////////////////////////////////////////////////////////////////////////////
// IncomingDataInfo for dispatch incoming data to client.
///////////////////////////////////////////////////////////////////////////////
class IncomingDataInfo{
public:
	IncomingDataInfo(const std::string tunnelID, char* data, size_t len) :
						tunnel_id(tunnelID),
						data_(data),
						len_(len) {}
	const std::string TunnelID() { return tunnel_id; }
	char* Data() { return data_; }
	size_t Len() { return len_; }
private:
	const std::string tunnel_id;
	char* data_;
	size_t len_;
};

///////////////////////////////////////////////////////////////////////////////
// ErrorInfo
///////////////////////////////////////////////////////////////////////////////
class ErrorInfo{
public:
	ErrorInfo(const std::string tunnelID, const ErrorCode errorCode, const std::string errDesc) :
										tunnel_id(tunnelID),
										err_code(errorCode),
										err_description(errDesc) {}
    const std::string TunnelID() { return tunnel_id; }
	const ErrorCode ErrCode() { return err_code; }
	const std::string ErrDesciption() { return err_description; }
private:
	const std::string tunnel_id;
	const ErrorCode err_code;
	const std::string err_description;
};

///////////////////////////////////////////////////////////////////////////////
// Maps
///////////////////////////////////////////////////////////////////////////////
//[stream*, tunnelID]
typedef std::map<const talk_base::StreamInterface*, std::string> StreamTunnelMap;
//[tunnelID, steram*]
typedef std::map<const std::string, const talk_base::StreamInterface*> TunnelStreamMap;
//[tunnelID, TunnelCache*]
typedef std::map<const std::string, TunnelCache*> TunnelCacheMap;
//[sessionID, Stream*]
typedef std::map<const std::string, talk_base::StreamInterface*> SessionStreamMap;

///////////////////////////////////////////////////////////////////////////////
// TunnelDataTransferClient
///////////////////////////////////////////////////////////////////////////////
class TunnelDataTransferClient : public sigslot::has_slots<> {
public:
	TunnelDataTransferClient(buzz::XmppClient* xmppclient, bool isGoogleClient);
	
	~TunnelDataTransferClient();
	
	//Set xmpp server name;
	void SetXMPPServer( const std::string xmpp_server_name );

	//Triggered when received login state from server.
	void OnStateChange(buzz::XmppEngine::State state);

	//Reset STUN and TURN server address.
	void ResetJingleInfo(const JingleInfo jingleInfo);

	//Start a new tunnel to send data if tunnel is not exist.
	void SendData(const std::string tunnelID);

	//Add data to outgoing tunnel cache.
	void AddData(const char* data, const size_t len, const std::string tunnelID);

	//Triggered when received an incoming tunnel.
	void OnIncomingTunnel(cricket::TunnelSessionClient* sessionClient, buzz::Jid remoteJid, cricket::Session* session);

	//Triggered when event on outgoing tunnel stream.
	void OnOutgoingStreamEvent(talk_base::StreamInterface* stream, int event, int err);

	//Triggered when event on incoming tunnel stream.
	void OnIncomingStreamEvent(talk_base::StreamInterface* stream, int event, int err);

	//Node own jid.
	const buzz::Jid OwnID(){ return xmpp_client_->jid();}

	talk_base::Thread* SignalingThread() { return session_manager_->signaling_thread(); }

	void DoOutgoingTunnelGC();
	void DoIncomingTunnelGC();
	void DoTunnelGC( TunnelCacheMap& tunnel_cache_map,  TunnelStreamMap& tunnel_st_map, bool isOutgoing );

	sigslot::signal1<const std::string> SignalLoggedIn;
	sigslot::signal1<const std::string> SignalLoggedOut;
	sigslot::signal3<const char*, const size_t, std::string> SignalDataReceived;
	sigslot::signal1<const std::string> SignalDataSuccessfullySent;
	sigslot::signal3<const std::string, const ErrorCode, const std::string> SignalDataSentFailed;
	sigslot::signal3<const std::string, const ErrorCode, const std::string> SignalDataReceiveFailed;

private:
	//Triggered when received stun&turn info from server.
	void OnJingleInfo(const std::string & relay_token, const std::vector<std::string> &relay_addresses, const std::vector<talk_base::SocketAddress> &stun_addresses);

	//Triggered when signed in to server.
	void OnSignon();

	//Handling session errors.
	void OnSessionError( cricket::BaseSession* session, cricket::BaseSession::Error error );

	//Triggered when new session created for incoming/outgoing tunnel.
	void OnSessionCreated(cricket::Session* session, talk_base::StreamInterface* stream, bool is_incoming);

	//Triggered when data sent .
	void OnDataSent( talk_base::StreamInterface* stream, TunnelCache* outgoing_cache );

	//Triggered when failed to send data.
	void OnDatasendFailed(const std::string tunnelID, const ErrorCode err, const std::string errDesc);

	//Triggered when failed to read data from stream.
	void OnDataReceiveFailed(const std::string tunnelID, const ErrorCode err, const std::string errDesc);

	//Load stun and turn address.
	void LoadJingleInfo();

	//Write data from outgoing cache to stream.
	bool WriteToStream(talk_base::StreamInterface* stream, const std::string tunnelID, TunnelCache* outgoing_cache, ErrorCode& errCode);

	//Read data from stream to incoming cache.
	bool ReadFromStream(talk_base::StreamInterface* stream, const std::string tunnelID, TunnelCache* incoming_cache, ErrorCode& errCode);

	//Close stream. Will trigger tunnel destroy.
	void CloseStream(const talk_base::StreamInterface* stream, bool isIncoming);

	//Cleaning tunnels.
	void CleanAll();
	void CleanOutgoingTunnel(const talk_base::StreamInterface* stream);
	void CleanIncomingTunnel(const talk_base::StreamInterface* stream );

	//Remove data cache from an outgoing/incoming tunnel.
	void RemoveDataCache(DataCache* cache);
	
	//Triggered when tunnel closed on errors.
	void OnTunnelClosedWithErrors(const talk_base::StreamInterface* stream, int err);
	
	//Triggered when recv stream close.
	void OnStreamClose( const std::string tunnelID, talk_base::StreamInterface* stream, int err, bool is_incoming );

	//Get outgoing/incoming tunnel cache from tunnel id.
	TunnelCache* GetTunnelCacheByID(const std::string tunnelID, const bool is_outgoing);
	
	//Process incoming data from recv buf.
	void ProcessIncomingData( size_t& offset, const size_t count, TunnelCache* incoming_cache);
	
	//Process incoming data len from recv buf.
	void ProcessLen( const std::string tunnelID, size_t& offset, const size_t count, DataCache* data_cache );

	//Process data from recv buf.
	void ProcessData( const std::string tunnelID, size_t& offset, const size_t count, DataCache* data_cache );

	//Triggered when a completed data received from the tunnel.
	void OnReceivedCompleteDataBlock( const std::string tunnelID, DataCache* data_cache );

	buzz::Jid fromNodeID(const std::string id);
	std::string toNodeID( const buzz::Jid jid );

	//Clear maps.
	void CleanStreamTunnelMap( StreamTunnelMap stream_map );
	void CleanTunnelStreamMap( TunnelStreamMap tunnel_st_map );
	void CleanTunnelCacheMap( TunnelCacheMap cache );
	void CleanSessionStreamMap( SessionStreamMap session_st_map );
	void OnDataReceived( char* data, const size_t len, const std::string tunnelID );
	
	talk_base::NetworkManager network_manager_; 
	talk_base::scoped_ptr<cricket::SessionManager> session_manager_;
	talk_base::scoped_ptr<cricket::SessionManagerTask> session_manager_task;
	talk_base::scoped_ptr<cricket::HttpPortAllocator> port_allocator_;
	talk_base::scoped_ptr<cricket::TunnelSessionClient> session_client_;
	talk_base::scoped_ptr<buzz::XmppClient> xmpp_client_;
	
	talk_base::Thread* worker_thread_;

	TunnelCacheMap outgoing_tunnel_map;
	TunnelCacheMap incoming_tunnel_map;
	StreamTunnelMap outgoing_st_map;
	StreamTunnelMap incoming_st_map;
	TunnelStreamMap outgoing_tunnel_st_map;
	TunnelStreamMap incoming_tunnel_st_map;
	SessionStreamMap outgoing_session_st_map;
	SessionStreamMap incoming_session_st_map;

	talk_base::scoped_ptr<OutgoingTunnelGCTask> outgoing_tunnel_gc_task;
	talk_base::scoped_ptr<IncomingTunnelGCTask> incoming_tunnel_gc_task;

	//Read buffer for each time data pulled from read stream.
	char read_buf[READ_BUF_SIZE];
	//Critical section for locking read/write pending map when send/receive data.
	mutable talk_base::CriticalSection cs_;
	//Stun and Relay info.
	JingleInfo jingle_info;
	//Only for test with google account.
	bool is_google_client;
	//xmpp server
	std::string xmpp_server;
};
#endif