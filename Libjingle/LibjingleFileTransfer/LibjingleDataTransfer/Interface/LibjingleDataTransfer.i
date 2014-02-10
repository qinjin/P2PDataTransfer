%module(directors="1") LibjingleDataTransfer

%{
#include "../datatransferappproxy.h"
%}

%include "windows.i"
%include "std_string.i"
%include "various.i"

/* Enum converter
 * Note: when you modified enum value in C++ code, don't forget to modify declaration here!
 */
%include "enums.swg"
%javaconst(1);

enum ErrorCode{
	//Session errors.
	SessionTimeOut,
	SessionErrors,

	//Stream errors.
	StreamReadError,
	StreamWriteError,

	//Validation errors.
	InvalidReceiver,
	MalformedID,

	//Status errors.
	ReceiverUnavailable,

	//Tunnel error notified and closed from another peer.
	NotifiedTunnelErrors,

	//Xmpp server connection timeout
	LoginTimeout
};

/* turn on director wrapping */
%feature("director") ReceiverCallback;

%include "../datatransferappproxy.h"