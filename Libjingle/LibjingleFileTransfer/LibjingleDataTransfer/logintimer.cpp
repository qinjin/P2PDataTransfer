#include "logintimer.h"

LoginTimer::LoginTimer( int timeOut, std::string nodeID, DataTransferApp* dtApp ) : time_out(timeOut), node_id(nodeID), datatransferApp(dtApp)
{
	timer_thread.reset(new talk_base::Thread());
	timer_thread->Start();
}

void LoginTimer::OnMessage( talk_base::Message *msg )
{
	switch(msg->message_id){
	case LOGIN_CANCEL:
		datatransferApp->SignalLoginFailed(node_id, "Couldn't connect to server.", ServerConnectionErrors);
		datatransferApp->logout();
		break;
	}
}

void LoginTimer::Start()
{
	LOG_F(INFO) << "Login timer started.";
	timer_thread->PostDelayed(time_out, this, LOGIN_CANCEL);
}

void LoginTimer::Stop()
{
	LOG_F(INFO) << "Login timer stopped.";
	timer_thread->Clear(this, LOGIN_CANCEL);
	timer_thread->Stop();
}