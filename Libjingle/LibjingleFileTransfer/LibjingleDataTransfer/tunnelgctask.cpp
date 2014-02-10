#include "tunnelgctask.h"

///////////////////////////////////////////////////////////////////////////////
// TunnelGCTask
///////////////////////////////////////////////////////////////////////////////
void TunnelGCTask::OnMessage( talk_base::Message *msg ) {
	std::string tunnel = IsIncomingTunnelGC() ? "Incoming" : "Outgoing"; 
	switch(msg->message_id){
	case START_GC:
		LOG(INFO)<<"["<<tunnel<<" Tunnel-GC] started";
		gc_thread->PostDelayed(time_out, this, TRIGGER_GC);
		break;
	case TRIGGER_GC:
		//Clear all the old GC msg and schedule a new one.
		gc_thread->Clear(this, TRIGGER_GC);
		gc_thread->PostDelayed(time_out, this, TRIGGER_GC);
		//Tell signal thread to do GC.
		client->SignalingThread()->Post(this, DO_GC);
		break;
	case STOP_GC:
		gc_thread->Stop();
		break;
	case DO_GC:
		DoTunnelGC();
	}
}

void TunnelGCTask::Start() {
	ASSERT(gc_thread.get());
	gc_thread->Start();
	gc_thread->Post(this, START_GC);
}

void TunnelGCTask::Stop() {
	gc_thread->Post(this, STOP_GC);
}

///////////////////////////////////////////////////////////////////////////////
// OutgoingTunnelGCTask
///////////////////////////////////////////////////////////////////////////////
void OutgoingTunnelGCTask::DoTunnelGC() {
	LOG(INFO)<<"[Outgoing Tunnel-GC] do GC...";
	client->DoOutgoingTunnelGC();
}

///////////////////////////////////////////////////////////////////////////////
// IncomingTunnelGCTask
///////////////////////////////////////////////////////////////////////////////
void IncomingTunnelGCTask::DoTunnelGC() {
	LOG(INFO)<<"[Incoming Tunnel-GC] do GC...";
	client->DoIncomingTunnelGC();
}
