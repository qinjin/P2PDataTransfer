#ifndef LOGIN_TIMER_H__
#define LOGIN_TIMER_H__
#include "datatransferapp.h"

enum LOGIN_TIMER_MSG {
	LOGIN_CANCEL
};

class LoginTimer : public talk_base::MessageHandler{
public:
	LoginTimer(int timeOut, std::string nodeID, DataTransferApp* dtApp);
	virtual ~LoginTimer() {}
	void OnMessage(talk_base::Message *msg);
	void Start();
	void Stop();
private:
	const int time_out;
	const std::string node_id;
	talk_base::scoped_ptr<talk_base::Thread> timer_thread;
	DataTransferApp* datatransferApp;
};

#endif