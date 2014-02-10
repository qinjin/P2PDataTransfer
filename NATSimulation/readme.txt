##########################################################################################################################
Programme command arguments: LibjingleDataTransfer.dll path
Each node should change nodeID in node.properties to its id, and each node will read receiver id from shared.properties.
##########################################################################################################################


##########################################################################################################################
Result csv file format:

send report:
receiver, sendStartTime, firstSentTime, sendEndTime, lastSentTime, numSent, numFailed, numTotal

recv report:
sender, recvStartTime, recvEndTime, numReceived, numFailed, numTotal
##########################################################################################################################

##########################################################################################################################
Note: Receiver should start before sender.
      You need to specify absolute LibjingleDataTransfer.dll path in run sender.bat and run receiver.bat.
##########################################################################################################################