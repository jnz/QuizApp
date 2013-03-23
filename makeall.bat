@echo off
call make.bat
echo 4 - Updating documentation...
cd javadoc
javadoc -private ..\src\*.java
cd ..
echo Complete.
