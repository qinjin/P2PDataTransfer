#ifndef CONSOLE_PROXY_H__
#define CONSOLE_PROXY_H__

#include "datatransferapp.h"
#include "console.h"

class ConsoleProxy{
public:
	__declspec(dllexport) ConsoleProxy() : console(0), client_thread(0) {}
	__declspec(dllexport) void StartConsole(DataTransferAppImpl* app, std::string& server, int numData, int dataSize);
	__declspec(dllexport) virtual ~ConsoleProxy(){
		delete console;
		delete client_thread;
	}
private:
	Console* console;
	talk_base::Thread* client_thread;
};

#endif