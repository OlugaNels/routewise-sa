@echo off
setlocal
cd /d "%~dp0"

netstat -ano | findstr /r /c:":3000 .*LISTENING" >nul
if errorlevel 1 (
    start "Sentinel Server" /min cmd /c "node server.js"
    timeout /t 2 /nobreak >nul
)

start "" "http://localhost:3000/"
endlocal
