#include "talk/examples/login/jingleinfotask.h"
#include "talk/base/socketaddress.h"
#include "talk/xmpp/constants.h"
#include "talk/xmpp/xmppclient.h"
#include "talk/base/logging.h"


namespace buzz {

class JingleInfoTask::JingleInfoGetTask : public XmppTask {
public:
  JingleInfoGetTask(Task * parent) : XmppTask(parent, XmppEngine::HL_SINGLE),
    done_(false) {}

  virtual int ProcessStart() {
    talk_base::scoped_ptr<buzz::XmlElement> get(MakeIq(STR_GET, JID_EMPTY, task_id()));
    get->AddElement(new XmlElement(QN_JINGLE_INFO_QUERY, true));
    if (SendStanza(get.get()) != XMPP_RETURN_OK) {
      return STATE_ERROR;
    }
    return STATE_RESPONSE;
  }
  virtual int ProcessResponse() {
    if (done_)
      return STATE_DONE;
    return STATE_BLOCKED;
  }

protected:
  virtual bool HandleStanza(const XmlElement * stanza) {
    if (!MatchResponseIq(stanza, JID_EMPTY, task_id()))
      return false;

    if (stanza->Attr(QN_TYPE) != STR_RESULT)
      return false;

    // Queue the stanza with the parent so these don't get handled out of order
    JingleInfoTask* parent = static_cast<JingleInfoTask*>(GetParent());
    parent->QueueStanza(stanza);

    // Wake ourselves so we can go into the done state
    done_ = true;
    Wake();
    return true;
  }

  bool done_;
};


void JingleInfoTask::RefreshJingleInfoNow() {
  JingleInfoGetTask* get_task = new JingleInfoGetTask(this);
  get_task->Start();
}

bool
JingleInfoTask::HandleStanza(const XmlElement * stanza) {
  if (!MatchRequestIq(stanza, "set", QN_JINGLE_INFO_QUERY))
    return false;

  // only respect relay push from the server
  Jid from(stanza->Attr(QN_FROM));
  if (from != JID_EMPTY &&
      !from.BareEquals(GetClient()->jid()) &&
      from != Jid(GetClient()->jid().domain()))
    return false;

  QueueStanza(stanza);
  return true;
}

int
JingleInfoTask::ProcessStart() {
  std::vector<std::string> relay_hosts;
  std::vector<talk_base::SocketAddress> stun_hosts;
  std::string relay_token;
  const XmlElement * stanza = NextStanza();
  if (stanza == NULL)
    return STATE_BLOCKED;
  const XmlElement * query = stanza->FirstNamed(QN_JINGLE_INFO_QUERY);
  if (query == NULL)
    return STATE_START;
  const XmlElement *stun = query->FirstNamed(QN_JINGLE_INFO_STUN);
  if (stun) {
	LOG(INFO) << "[JingleInfoTask::ProcessStart()] In getting stun info" << std::endl;
    for (const XmlElement *server = stun->FirstNamed(QN_JINGLE_INFO_SERVER);
         server != NULL; server = server->NextNamed(QN_JINGLE_INFO_SERVER)) {
      std::string host = server->Attr(QN_JINGLE_INFO_HOST);
      std::string port = server->Attr(QN_JINGLE_INFO_UDP);
      if (host != STR_EMPTY && host != STR_EMPTY)
	      stun_hosts.push_back(talk_base::SocketAddress(host, atoi(port.c_str())));
    }
  }
 
  const XmlElement *relay = query->FirstNamed(QN_JINGLE_INFO_RELAY);
  if (relay) {
	LOG(INFO) << "[JingleInfoTask::ProcessStart()] In getting turn info" << std::endl;
    relay_token = relay->TextNamed(QN_JINGLE_INFO_TOKEN);
    for (const XmlElement *server = relay->FirstNamed(QN_JINGLE_INFO_SERVER);
         server != NULL; server = server->NextNamed(QN_JINGLE_INFO_SERVER)) {
      std::string host = server->Attr(QN_JINGLE_INFO_HOST);
      if (host != STR_EMPTY) {
        relay_hosts.push_back(host);
      }
    }
  }

  LOG(INFO) << "[JingleInfoTask::ProcessStart()] Getting stun/turn info done, dispatch JingleInfo singal." << std::endl;
  SignalJingleInfo(relay_token, relay_hosts, stun_hosts);
  return STATE_START;
}


}



