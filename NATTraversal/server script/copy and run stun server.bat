@echo off
copy ..\..\Libjingle\libjingle-0.5.8\talk\build\dbg\staging\stunserver.exe .\ /y
stunserver 130.229.172.227:19293
cmd