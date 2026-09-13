@echo off
rem Switch current CMD session to JDK25 (zhenxinjian requires 25; system JAVA_HOME stays JDK8 for another project)
rem Usage: tools\env\jdk25.cmd   (affects current session only)
rem Resolution: ZXJ_JDK25_HOME env var wins; falls back to local default below.
if "%ZXJ_JDK25_HOME%"=="" set ZXJ_JDK25_HOME=D:\App\Java\jdk-25.0.4.1
if not exist "%ZXJ_JDK25_HOME%\bin\java.exe" (
    echo [jdk25] ERROR: java.exe not found under %ZXJ_JDK25_HOME%
    echo [jdk25] Set ZXJ_JDK25_HOME to your JDK25 root, e.g. set ZXJ_JDK25_HOME=D:\App\Java\jdk-25.0.4.1
    exit /b 1
)
set JAVA_HOME=%ZXJ_JDK25_HOME%
set PATH=%ZXJ_JDK25_HOME%\bin;%PATH%
echo [jdk25] JAVA_HOME=%JAVA_HOME%
java -version
