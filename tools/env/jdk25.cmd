@echo off
rem Switch current CMD session to JDK25 (zhenxinjian requires 25; system JAVA_HOME stays JDK8 for another project)
rem Usage: tools\env\jdk25.cmd   (affects current session only)
set JAVA_HOME=D:\App\java\25
set PATH=D:\App\java\25\bin;%PATH%
echo [jdk25] JAVA_HOME=%JAVA_HOME%
java -version
