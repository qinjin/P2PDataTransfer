#ifndef _DATATRANSFER_CONSTANTS_H_
#define _DATATRANSFER_CONSTANTS_H_

#ifndef WIN32
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <iomanip>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/wait.h>
#else
#include <direct.h>
#include "talk/base/win32.h"
#endif

#include "talk/xmllite/qname.h"

////////////////////////////////////////////////////////////////////////////////
////////////////////////Tunnel session description//////////////////////////////
///////////////////////for data  with "tunnel id"= 3333/////////////////////////
// <description xmlns='http://www.degoo.com/tunnel'>
//   <type>
//		data
//   </type>
// </description>
///////////////////////////////////////////////////////////////////////////////

//Jid
extern const std::string JID_RESOURCE;

//default jingle info.
extern const std::string DEFAULT_STUN_ADDR;
extern const int DEFAULT_STUN_PORT;
extern const std::string DEFAULT_RELAY_ADDR;
extern const int DEFAULT_RELAY_PORT;
extern const std::string DEFAULT_RELAY_TOKEN;

//GC timeout.
extern const long OUTGOING_GC_TIMEOUT;
extern const long INCOMING_GC_TIMEOUT; 

#endif