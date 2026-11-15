@echo off
rem Builds a Windows app folder with AI Game Master.exe that needs no Java installed and no commands to run.
rem Requires: JDK 17+ (with jpackage) and Maven on the PATH. Run from the project folder.
call mvn -q package -DskipTests
if errorlevel 1 goto fail
if exist dist rmdir /s /q dist
if exist target\app rmdir /s /q target\app
mkdir target\app
copy /y target\AIGameMaster.jar target\app\ >nul
jpackage --type app-image --name "AI Game Master" --input target\app --main-jar AIGameMaster.jar --main-class gamemaster.gui.GuiApp --dest dist
if errorlevel 1 goto fail
if exist .env copy /y .env "dist\AI Game Master\.env" >nul
echo.
echo Done. Open dist\AI Game Master\ and double-click "AI Game Master.exe".
echo Your .env was copied next to the exe (if you have one). Never share that folder: it holds your API key.
goto end
:fail
echo Build failed. See the messages above.
:end
pause
