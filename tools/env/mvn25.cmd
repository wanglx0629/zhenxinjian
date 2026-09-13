@echo off
rem Run Maven with JDK25 (system JAVA_HOME untouched, effective in child process only)
rem Usage: tools\env\mvn25.cmd test -Dtest=BodyCalcServiceTest
rem        tools\env\mvn25.cmd spring-boot:run
rem Resolution: ZXJ_JDK25_HOME env var wins; falls back to local default below.
setlocal
if "%ZXJ_JDK25_HOME%"=="" set ZXJ_JDK25_HOME=D:\App\Java\jdk-25.0.4.1
if not exist "%ZXJ_JDK25_HOME%\bin\java.exe" (
    echo [mvn25] ERROR: java.exe not found under %ZXJ_JDK25_HOME%
    echo [mvn25] Set ZXJ_JDK25_HOME to your JDK25 root, e.g. set ZXJ_JDK25_HOME=D:\App\Java\jdk-25.0.4.1
    exit /b 1
)
set JAVA_HOME=%ZXJ_JDK25_HOME%
mvn %*
endlocal
