@echo off
setlocal

cd /d %~dp0\..

set APP_NAME=Assinador ICP-Brasil
set APP_VERSION=1.0.0
set MAIN_JAR=assinador-ipc-brasil-1.0.0-SNAPSHOT-all.jar
set MAIN_CLASS=br.com.assinador.Main
set DIST_DIR=dist
set IMAGE_DIR=%DIST_DIR%\image
set INSTALLER_DIR=%DIST_DIR%\installer
set PORTABLE_DIR=%DIST_DIR%\portable
set EXE_NAME=Assinador-ICP-Brasil-Setup.exe

where jpackage >nul 2>nul
if errorlevel 1 (
  echo [package] jpackage nao encontrado. Use JDK 17+ com jpackage no PATH.
  exit /b 1
)

if not exist target\%MAIN_JAR% (
  echo [package] JAR com dependencias nao encontrado. Executando build...
  call mvn clean package -DskipTests
  if errorlevel 1 (
    echo [package] Falha no build Maven.
    exit /b 1
  )
)

if not exist target\%MAIN_JAR% (
  echo [package] JAR com dependencias ainda nao encontrado em target\%MAIN_JAR%.
  exit /b 1
)

if not exist %DIST_DIR% mkdir %DIST_DIR%
if exist %IMAGE_DIR% rmdir /s /q %IMAGE_DIR%
if exist %INSTALLER_DIR% rmdir /s /q %INSTALLER_DIR%
if exist %PORTABLE_DIR% rmdir /s /q %PORTABLE_DIR%
mkdir %IMAGE_DIR%
mkdir %INSTALLER_DIR%
mkdir %PORTABLE_DIR%

echo [package] Gerando app-image com runtime Java embutido...
jpackage ^
  --type app-image ^
  --name "%APP_NAME%" ^
  --dest "%IMAGE_DIR%" ^
  --input target ^
  --main-jar "%MAIN_JAR%" ^
  --main-class "%MAIN_CLASS%" ^
  --app-version "%APP_VERSION%" ^
  --vendor "Assinador ICP-Brasil" ^
  --description "Assinador PDF ICP-Brasil A3 via PKCS#11"
if errorlevel 1 (
  echo [package] Falha ao gerar app-image.
  exit /b 1
)

echo [package] Gerando instalador EXE para usuario final...
jpackage ^
  --type exe ^
  --name "Assinador-ICP-Brasil-Setup" ^
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
  echo [package] Falha ao gerar instalador EXE.
  exit /b 1
)

echo [package] Organizando nome do instalador...
if exist "%INSTALLER_DIR%\Assinador-ICP-Brasil-Setup-%APP_VERSION%.exe" (
  move /y "%INSTALLER_DIR%\Assinador-ICP-Brasil-Setup-%APP_VERSION%.exe" "%INSTALLER_DIR%\%EXE_NAME%" >nul
)

for /d %%D in ("%IMAGE_DIR%\%APP_NAME%") do (
  echo [package] Gerando versao portatil ZIP...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%%~fD\\*' -DestinationPath '%PORTABLE_DIR%\\Assinador-ICP-Brasil-Portable.zip' -Force"
)

echo [package] Empacotamento concluido.
echo [package] Instalador: %INSTALLER_DIR%\%EXE_NAME%
echo [package] Portatil:  %PORTABLE_DIR%\Assinador-ICP-Brasil-Portable.zip
endlocal
