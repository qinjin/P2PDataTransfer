#ifndef DATTRANSFER_CONSOLE_H__
#define DATTRANSFER_CONSOLE_H__

#include "datatransferapp.h"

enum {
	CONSOLE_START, 
	CONSOLE_INPUT
};

class Console : public talk_base::MessageHandler {
public:
	Console(DataTransferAppImpl* app, std::string& xmppServer, int numData, int dataSize) :
			datatransfer_app(app), 
			xmpp_server(xmppServer),
			num_data(numData),
			data_size(dataSize) {
				data_v.reserve(dataSize);
			}
	void OnMessage(talk_base::Message *msg);
	virtual ~Console() {}

private:
	void Print(const char* str);
	void Print(const std::string& str);
	void ParseLine(std::string &str);
	void StartConsole();
	void LoadData();
	DataTransferAppImpl* datatransfer_app;
	std::string xmpp_server;
	const int data_size;
	const int num_data;
	std::vector<char> data_v;
};

#endif
