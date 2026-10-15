@echo off
set OLLAMA_NO_CLOUD=1
ollama serve
exit /b %errorlevel%
