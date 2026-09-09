@echo off
rem Run Maven with JDK25 (system JAVA_HOME untouched, effective in child process only)
rem Usage: tools\env\mvn25.cmd test -Dtest=BodyCalcServiceTest
rem        tools\env\mvn25.cmd spring-boot:run
setlocal
set JAVA_HOME=D:\App\java\25
mvn %*
endlocal
