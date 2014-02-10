#include "constants.h"

const std::string JID_RESOURCE("data");

const std::string DEFAULT_STUN_ADDR("130.229.165.240");
const int DEFAULT_STUN_PORT = 19293;
const std::string DEFAULT_RELAY_ADDR("130.229.165.240");
const int DEFAULT_RELAY_PORT = 80;
const std::string DEFAULT_RELAY_TOKEN("");

//TODO: Adjust timeout to a proper value after test.
const long OUTGOING_GC_TIMEOUT = 1000 * 20 * 1;            // 60 seconds
const long INCOMING_GC_TIMEOUT = 1000 * 40 * 2;            // 120 seconds