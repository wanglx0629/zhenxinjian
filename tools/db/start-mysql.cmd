@echo off
rem ===========================================================================
rem start-mysql.cmd - Ensure the local MySQL service (MySQL80) is running.
rem MySQL80 is normally StartMode=Auto, so this is usually a no-op.
rem Starting a service requires an elevated shell; the script prints a hint
rem when net start fails. ASCII only in .cmd files - see README.md.
rem ===========================================================================
setlocal

set SVC_NAME=MySQL80
set MYSQL_HOME=D:\App\MySQL\MySQLServer8
set MYSQL_PORT=3306

netstat -ano | findstr LISTENING | findstr ":%MYSQL_PORT% " >nul 2>&1
if %errorlevel%==0 (
  echo [mysql] already listening on 127.0.0.1:%MYSQL_PORT%, nothing to do.
  exit /b 0
)

echo [mysql] port %MYSQL_PORT% down, starting service %SVC_NAME% ...
net start %SVC_NAME%
if errorlevel 1 (
  echo [mysql] ERROR: failed to start %SVC_NAME%. Re-run this script as Administrator.
  exit /b 1
)

echo [mysql] service %SVC_NAME% started.
echo [mysql] client: "%MYSQL_HOME%\bin\mysql.exe" -h127.0.0.1 -uroot -p zhenxinjian
exit /b 0
