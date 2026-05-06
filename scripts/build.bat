@echo off
setlocal

cd /d %~dp0\..

echo [build] Limpando e compilando projeto...
call mvn clean package -DskipTests
if errorlevel 1 (
  echo [build] Falha ao compilar o projeto.
  exit /b 1
)

echo [build] Build concluido com sucesso.
endlocal
