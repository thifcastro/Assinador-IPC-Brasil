@echo off
setlocal

cd /d %~dp0\..

set APP_NAME=Assinador ICP-Brasil
set APP_VERSION=1.0.0
set MAIN_JAR=assinador-ipc-brasil-1.0.0-SNAPSHOT.jar
set MAIN_CLASS=br.com.assinador.Main
set DIST_DIR=dist
set IMAGE_DIR=%DIST_DIR%\image
set INSTALLER_DIR=%DIST_DIR%\installer

where jpackage >nul 2>nul
if errorlevel 1 (
  echo [package] jpackage nao encontrado. Use JDK 17+ com jpackage no PATH.
  exit /b 1
)

if not exist target\%MAIN_JAR% (
  echo [package] JAR nao encontrado. Executando build...
  call mvn clean package -DskipTests
  if errorlevel 1 (
    echo [package] Falha no build Maven.
    exit /b 1
  )
)

if not exist %DIST_DIR% mkdir %DIST_DIR%
if exist %IMAGE_DIR% rmdir /s /q %IMAGE_DIR%
if exist %INSTALLER_DIR% rmdir /s /q %INSTALLER_DIR%
mkdir %IMAGE_DIR%
mkdir %INSTALLER_DIR%

echo [package] Gerando app-image com runtime embutido...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --dest "%IMAGE_DIR%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Assinador ICP-Brasil" ^
  --description "Assinador PDF ICP-Brasil A3 via PKCS#11" ^
  --win-shortcut ^
  --win-menu
if errorlevel 1 (
  echo [package] Falha ao gerar app-image.
  exit /b 1
)

echo [package] Gerando instalador EXE...
jpackage ^
  --type exe ^
  --name "%APP_NAME%" ^
  --dest "%INSTALLER_DIR%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Assinador ICP-Brasil" ^
  --description "Assinador PDF ICP-Brasil A3 via PKCS#11" ^
  --win-shortcut ^
  --win-menu
if errorlevel 1 (
  echo [package] Falha ao gerar instalador EXE.\nTentando MSI...
  jpackage ^
    --type msi ^
    --name "%APP_NAME%" ^
    --dest "%INSTALLER_DIR%" ^
    --input target ^
    --main-jar "%MAIN_JAR%" ^
    --main-class "%MAIN_CLASS%" ^
    --app-version "%APP_VERSION%" ^
    --vendor "Assinador ICP-Brasil" ^
    --description "Assinador PDF ICP-Brasil A3 via PKCS#11" ^
    --win-shortcut ^
    --win-menu
  if errorlevel 1 (
    echo [package] Falha ao gerar instalador EXE/MSI.
    exit /b 1
  )
)

echo [package] Empacotamento finalizado. Verifique a pasta %DIST_DIR%.
endlocal
