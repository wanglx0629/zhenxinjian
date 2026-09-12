@echo off
rem SQL runner entry: compile SqlRunner if needed, then run it
rem Usage: tools\db\run-sql.cmd ^<sql-file^>
rem Credentials: auto-loads db.local.properties next to this script (gitignored)
rem Note: change^<N^>_*.sql auto-recorded in schema_migrations, re-run auto-skipped; --allow-error removed (fail-fast)
setlocal
set DIR=%~dp0
set CP=D:\App\apache-maven-3.9.16\repository\com\mysql\mysql-connector-j\9.7.0\mysql-connector-j-9.7.0.jar
rem Always recompile (cheap, single file) so SqlRunner.java changes take effect immediately
D:\App\java\25\bin\javac -encoding UTF-8 -cp %CP% -d "%DIR%out" "%DIR%SqlRunner.java" || exit /b 1
if exist "%DIR%db.local.properties" (
    D:\App\java\25\bin\java -Dfile.encoding=UTF-8 -cp %CP%;%DIR%out SqlRunner %* "%DIR%db.local.properties"
) else (
    D:\App\java\25\bin\java -Dfile.encoding=UTF-8 -cp %CP%;%DIR%out SqlRunner %*
)
endlocal
