@echo off
rem ===========================================================================
rem start-infra.cmd - One shot: ensure MySQL (service) + Redis (console) are up
rem before launching the backend. ASCII only - see tools/README.md.
rem ===========================================================================
setlocal

set TOOLS_DIR=%~dp0

call "%TOOLS_DIR%db\start-mysql.cmd"
if errorlevel 1 exit /b 1

call "%TOOLS_DIR%redis\start-redis.cmd"
if errorlevel 1 exit /b 1

echo.
echo [infra] MySQL 127.0.0.1:3306 + Redis 127.0.0.1:6379 ready.
echo [infra] next: tools\env\mvn25.cmd spring-boot:run (in apps\zhenxinjian-backend)
exit /b 0
