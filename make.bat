@echo off
echo 1 - Compiling Quiz...
cd src
REM Compile for JRE 6, use UTF8 source file
REM Disable build warning for JRE 6 compilation: -Xlint:-options
REM Show warnings for deprecated stuff.
REM Show warnings for unchecked stuff.
javac -source 6 -target 6 -encoding utf8 -Xlint:-options -Xlint:deprecation -Xlint:unchecked -d ..\build\ *.java
cd ..
echo 2 - Creating jar file...
jar cfe quiz.jar javaquiz.Quiz -C build . -C img . -C sound . -C db .
echo 3 - Updating tags file...
ctags -R --c++-kinds=+p --fields=+iaS --extra=+q .
