@echo off
setlocal

cd /d %~dp0\..

set JAR=target\assinador-ipc-brasil-1.0.0-SNAPSHOT-all.jar
if not exist "%JAR%" (
  echo [run] JAR nao encontrado em %JAR%
  echo [run] Execute scripts\build.bat antes.
  exit /b 1
)

echo [run] Executando Assinador ICP-Brasil...
java -jar "%JAR%"

endlocal
