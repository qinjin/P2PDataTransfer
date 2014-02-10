@echo off
cd talk
..\swtoolkit\hammer.bat --jobs=6 --verbose --mode=all all_programs
cmd