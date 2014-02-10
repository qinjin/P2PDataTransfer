#include "consoleproxy.h"

void ConsoleProxy::StartConsole(DataTransferAppImpl* app, std::string& server, int numData, int dataSize){
	console = new Console(app, server, numData, dataSize);

	client_thread = new talk_base::Thread(app->SocketServer());
	client_thread->Start();
	client_thread->Post(console, CONSOLE_START);
}