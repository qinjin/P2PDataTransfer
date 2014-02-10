/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.4
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package com.degoo.libjingle4j.proxy;

class LibjingleDataTransferJNI {
  public final static native void delete_ReceiverCallback(long jarg1);
  public final static native void ReceiverCallback_onLoggedIn(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onLoggedInSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onLoggedOut(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onLoggedOutSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onLoginFailed(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native void ReceiverCallback_onLoginFailedSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataReceived(long jarg1, ReceiverCallback jarg1_, byte[] jarg2, long jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataReceivedSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, byte[] jarg2, long jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataReceiveFailed(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataReceiveFailedSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataSent(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onDataSentSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2);
  public final static native void ReceiverCallback_onDataSentFailed(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native void ReceiverCallback_onDataSentFailedSwigExplicitReceiverCallback(long jarg1, ReceiverCallback jarg1_, String jarg2, int jarg3, String jarg4);
  public final static native long new_ReceiverCallback();
  public final static native void ReceiverCallback_director_connect(ReceiverCallback obj, long cptr, boolean mem_own, boolean weak_global);
  public final static native void ReceiverCallback_change_ownership(ReceiverCallback obj, long cptr, boolean take_or_release);
  public final static native long new_DataTransferAppProxy();
  public final static native void delete_DataTransferAppProxy(long jarg1);
  public final static native void DataTransferAppProxy_login(long jarg1, DataTransferAppProxy jarg1_, String jarg2, String jarg3, String jarg4, int jarg5, int jarg6);
  public final static native void DataTransferAppProxy_logout(long jarg1, DataTransferAppProxy jarg1_);
  public final static native void DataTransferAppProxy_send(long jarg1, DataTransferAppProxy jarg1_, byte[] jarg2, long jarg3, String jarg4);
  public final static native void DataTransferAppProxy_setDebug(long jarg1, DataTransferAppProxy jarg1_, boolean jarg2);
  public final static native void DataTransferAppProxy_setStunAndRelayInfo(long jarg1, DataTransferAppProxy jarg1_, String jarg2, int jarg3, String jarg4, int jarg5);
  public final static native void DataTransferAppProxy_setReceiver(long jarg1, DataTransferAppProxy jarg1_, long jarg2, ReceiverCallback jarg2_);
  public final static native void DataTransferAppProxy_deleteReceiver(long jarg1, DataTransferAppProxy jarg1_);
  public final static native boolean DataTransferAppProxy_isReceiverInitialized(long jarg1, DataTransferAppProxy jarg1_);

  public static void SwigDirector_ReceiverCallback_onLoggedIn(ReceiverCallback self, String nodeID) {
    self.onLoggedIn(nodeID);
  }
  public static void SwigDirector_ReceiverCallback_onLoggedOut(ReceiverCallback self, String nodeID) {
    self.onLoggedOut(nodeID);
  }
  public static void SwigDirector_ReceiverCallback_onLoginFailed(ReceiverCallback self, String nodeID, int errCode, String errDesc) {
    self.onLoginFailed(nodeID, ErrorCode.swigToEnum(errCode), errDesc);
  }
  public static void SwigDirector_ReceiverCallback_onDataReceived(ReceiverCallback self, byte[] BYTE, long len, String remoteNodeID) {
    self.onDataReceived(BYTE, len, remoteNodeID);
  }
  public static void SwigDirector_ReceiverCallback_onDataReceiveFailed(ReceiverCallback self, String remoteNodeID, int errCode, String errDesc) {
    self.onDataReceiveFailed(remoteNodeID, ErrorCode.swigToEnum(errCode), errDesc);
  }
  public static void SwigDirector_ReceiverCallback_onDataSent(ReceiverCallback self, String remoteNodeID) {
    self.onDataSent(remoteNodeID);
  }
  public static void SwigDirector_ReceiverCallback_onDataSentFailed(ReceiverCallback self, String remoteNodeID, int errCode, String errDesc) {
    self.onDataSentFailed(remoteNodeID, ErrorCode.swigToEnum(errCode), errDesc);
  }

  private static native void swig_module_init();
  static {
    swig_module_init();
  }
}