#include <cstdlib>
#include "console.h"

const char* CONSOLE_DATA_CMD =
	"Available commands:\n"
	"\n"
	"  send <id> <data> <remote_node_id>    Send a data string to remote node.\n"
	"  sendfile <remote_node_id>            Send a test FILE to remote node.\n"
	"  quit                                 Quit the application.\n"
	"";

void Console::StartConsole() {
  char input_buffer[64];
  Print("Console started!");
  Print(CONSOLE_DATA_CMD);
  LoadData();
 
  for (;;) {
	fgets(input_buffer, sizeof(input_buffer), stdin);
	datatransfer_app->MainThread()->Post(this, CONSOLE_INPUT, new talk_base::TypedMessageData<std::string>(input_buffer));
  }
}

void Console::OnMessage(talk_base::Message *msg) {
	switch (msg->message_id) {
	case CONSOLE_START:
		StartConsole();
		break;
	case CONSOLE_INPUT:
		talk_base::TypedMessageData<std::string> *data = static_cast<talk_base::TypedMessageData<std::string>*>(msg->pdata);
		ParseLine(data->data());
		break;
	}
}

void Console::ParseLine(std::string &line) {
	std::vector<std::string> words;
	int start = -1;
	int state = 0;
	for (int index = 0; index < static_cast<int>(line.size()); ++index) {
		if (state == 0) {
			if (!isspace(line[index])) {
				start = index;
				state = 1;
			}
		} else {
			if(state != 1) Print("ERROR: In reading cmd, state != 1.");
			if(start < 0) Print("ERROR: In reading cmd, start < 0.");
			if (isspace(line[index])) {
				std::string word(line, start, index - start);
				words.push_back(word);
				start = -1;
				state = 0;
			}
		}
	}

	if ((words.size() == 1) && (words[0] == "quit")) {
		datatransfer_app->logout();
		Print("Quit program!");
		exit(0);
	} else if((words.size() == 2) && words[0] == "sendfile") {
		int numSend = 0;
		while(numSend < num_data){
			datatransfer_app->AddData(&data_v[0], data_size, (char*)words[1].c_str());
			numSend++;
		}
	} else if((words.size() == 3) && (words[0] == "send")){
		char* console_data = (char*)words[1].c_str();
		char* data = new char[strlen(console_data)+1];
		std::memcpy(data, console_data, strlen(console_data));
		data[strlen(console_data)] = '\0';
		datatransfer_app->AddData(data, strlen(data), (char*)words[2].c_str());
	} else{
		Print("ERROR Command:\n");
		Print(CONSOLE_DATA_CMD);
	}
}

void Console::Print(const char* str) {
  printf("\n%s", str);
}

void Console::Print(const std::string& str) {
  Print(str.c_str());
}

void Console::LoadData() {
	//fill data will character a.
	data_v.assign(data_size, 'a');
}