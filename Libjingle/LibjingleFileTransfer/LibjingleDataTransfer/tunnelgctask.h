#ifndef TUNNEL_GC_TASK_H__
#define TUNNEL_GC_TASK_H__
#include "tunneldatatransferclient.h"

class TunnelDataTransferClient;
///////////////////////////////////////////////////////////////////////////////
// GC Message
///////////////////////////////////////////////////////////////////////////////
enum GCMSG{
	START_GC,
	TRIGGER_GC,
	DO_GC,
	STOP_GC
};

///////////////////////////////////////////////////////////////////////////////
// TunnelGCTask
// Tunnel GC is for closing inactive tunnels in next GC scheduling.
///////////////////////////////////////////////////////////////////////////////
class TunnelGCTask : public talk_base::MessageHandler {
public:
	TunnelGCTask(TunnelDataTransferClient* tdt_client, long timeOut) :
		  client(tdt_client),
		  time_out(timeOut) {
			  gc_thread.reset(new talk_base::Thread());
		  }

	virtual ~TunnelGCTask() {}
	
	void OnMessage(talk_base::Message *msg);
	
	void Start();
	void Stop();

	virtual void DoTunnelGC() = 0;
	virtual bool IsIncomingTunnelGC() = 0;

protected:
	TunnelDataTransferClient* client;

private:
	long time_out;
	talk_base::scoped_ptr<talk_base::Thread> gc_thread;
};

///////////////////////////////////////////////////////////////////////////////
// OutgoingTunnelGCTask
///////////////////////////////////////////////////////////////////////////////
class OutgoingTunnelGCTask : public TunnelGCTask{
public: 
	OutgoingTunnelGCTask(TunnelDataTransferClient* tunne_client, long timeOut) : 
		TunnelGCTask(tunne_client, timeOut) {}

	virtual void DoTunnelGC();
	virtual bool IsIncomingTunnelGC() { return false; }
};

///////////////////////////////////////////////////////////////////////////////
// IncomingTunnelGCTask
///////////////////////////////////////////////////////////////////////////////
class IncomingTunnelGCTask : public TunnelGCTask{
public:
	IncomingTunnelGCTask(TunnelDataTransferClient* tunne_client, long timeOut) : 
		TunnelGCTask(tunne_client, timeOut) {}

	virtual void DoTunnelGC();
	virtual bool IsIncomingTunnelGC() { return true; }
};

#endif

