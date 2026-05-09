@echo off
cd /d "%~dp0..\cognistream-backend"
call mvnw.cmd spring-boot:run
exit /b %errorlevel%
