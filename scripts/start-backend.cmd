@echo off
if not defined JAVA_HOME (
    for /d %%J in ("%USERPROFILE%\.jdks\ms-21*") do (
        if exist "%%~fJ\bin\java.exe" set "JAVA_HOME=%%~fJ"
    )
)
cd /d "%~dp0..\cognistream-backend"
call mvnw.cmd spring-boot:run
exit /b %errorlevel%
