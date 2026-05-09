@echo off
cd /d "%~dp0..\client"
if not exist node_modules call npm.cmd ci
if errorlevel 1 exit /b %errorlevel%
call npm.cmd run dev
exit /b %errorlevel%
