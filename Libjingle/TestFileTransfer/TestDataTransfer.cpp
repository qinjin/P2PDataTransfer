#include <map>
#include <math.h>
#include "consoleproxy.h"

typedef std::pair <std::string, std::string> UserPassword;

UserPassword getSelectedUser(std::map<std::string, std::string> userMap, std::string firstUser, std::string secondUser) {
	std::string selectedIndex;
	std::cout<<"Select a user: [1|2] '";
	std::map<std::string, std::string>::iterator iter;
	for(iter =userMap.begin(); iter!=userMap.end(); iter++){
		std::cout<<(*iter).first<<"' ";
	}
	std::cout<<std::endl;
	std::cin>>selectedIndex;
	while(selectedIndex.compare("1") !=0 && selectedIndex.compare("2") !=0){
		std::cout<<"Wrong selection, try again [1|2]: ";
		std::cin>> selectedIndex;
	}

	std::string userName = selectedIndex.compare("1") == 0 ? firstUser : secondUser;
	std::string password = (*userMap.find(userName)).second;

	return UserPassword(userName, password);
}

LoginInfo* initLoginInfo() 
{

	std::string user_name;
	std::string pass_word;
	std::string xmpp_server_name;
	int xmpp_server_port;
	std::map<std::string, std::string> google_user_map;
	std::map<std::string, std::string> local_user_map;
	//google_user_map["libjingle.tests@gmail.com"] = "Libjingle";
	//google_user_map["libjingle.tester0@gmail.com"] = "1Stockholm";
	//user_map["libjingle.tester2@gmail.com"] = "1Stockholm";

	local_user_map["libjingle.tester0"] = "111111";
	local_user_map["libjingle.tester1"] = "111111";

	UserPassword selectedUser = getSelectedUser(local_user_map, "libjingle.tester0", "libjingle.tester1");
	user_name = selectedUser.first;
	pass_word = selectedUser.second;
	xmpp_server_name = "localhost";
	xmpp_server_port = 5222;

	std::cout<<"Test with local server on port 5222. user: "<< user_name <<std::endl;

	LoginInfo* login_info = new LoginInfo(user_name, pass_word, xmpp_server_name, xmpp_server_port);
	return login_info;
}

void simpleArraryTest();
void CeilTest();


int main(int argc, char **argv){
	LoginInfo* login_info = initLoginInfo();
	DataTransferAppImpl* app = new DataTransferAppImpl();
	ConsoleProxy* console_proxy = new ConsoleProxy();

	std::cout<<"Open debug? [Y|N]: ";
	std::string debugCmd;
	std::cin>>debugCmd;
	app->SetDebugMode((debugCmd.compare("Y")==0 || debugCmd.compare("y")==0) ? true : false);
	std::cout<<std::endl;

	std::cout << "Number of data to transfer:";
	int numData;
	std::cin >> numData;
	std::cout<<std::endl;

	std::cout << "Data size(KB): ";
	int dataSize;
	std::cin >> dataSize;
	std::cout<<std::endl;

	std::cout<<"Transfer " << numData <<" with size = "<< dataSize <<" KB."<<std::endl;

	std::string server = login_info->Servername();
	console_proxy->StartConsole(app, server, numData, dataSize);

	std::cout<<"user_name = "<<login_info->Username();
	app->login((char*)login_info->Username().c_str(), (char*)login_info->Password().c_str(), (char*)login_info->Servername().c_str(), login_info->ServerPort());

	app->~DataTransferAppImpl();

	return 0;
}