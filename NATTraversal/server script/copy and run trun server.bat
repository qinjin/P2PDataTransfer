@echo off
copy ..\..\Libjingle\libjingle-0.5.8\talk\build\dbg\staging\relayserver.exe .\ /y
relayserver 127.0.0.1:80 130.229.172.227:80
cmd