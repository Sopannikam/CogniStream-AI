@echo off
ollama pull nomic-embed-text:latest
if errorlevel 1 exit /b %errorlevel%
ollama pull qwen2.5-coder:1.5b
exit /b %errorlevel%
