@echo off
rem ===========================================================================
rem start-redis.cmd - Start the local GREEN Redis install for zhenxinjian.
rem Redis has NO Windows service here; it runs as a console process.
rem Password comes from redis.windows-service.conf (requirepass); this script
rem never embeds the password. ASCII only in .cmd files - see README.md.
rem ===========================================================================
setlocal

set REDIS_HOME=D:\App\Redis
set REDIS_EXE=%REDIS_HOME%\redis-server.exe
set REDIS_CONF=redis.windows-service.conf
set REDIS_PORT=6379

if not exist "%REDIS_EXE%" (
  echo [redis] ERROR: %REDIS_EXE% not found.
  exit /b 1
)

netstat -ano | findstr LISTENING | findstr ":%REDIS_PORT% " >nul 2>&1
if %errorlevel%==0 (
  echo [redis] already listening on 127.0.0.1:%REDIS_PORT%, nothing to do.
  exit /b 0
)

echo [redis] starting console Redis in a new window...
start "zxj-redis" /D "%REDIS_HOME%" "%REDIS_EXE%" %REDIS_CONF%

rem Wait up to ~15s for the port to come up.
set /a TRIES=0
:wait_loop
set /a TRIES+=1
ping -n 2 127.0.0.1 >nul 2>&1
netstat -ano | findstr LISTENING | findstr ":%REDIS_PORT% " >nul 2>&1
if %errorlevel%==0 goto :up
if %TRIES% GEQ 10 goto :timeout
goto :wait_loop

:up
echo [redis] UP on 127.0.0.1:%REDIS_PORT% (db 0).
echo [redis] client: %REDIS_HOME%\redis-cli.exe -a ^<password^> ping
exit /b 0

:timeout
echo [redis] TIMEOUT waiting for port %REDIS_PORT%, check the zxj-redis window.
exit /b 1
