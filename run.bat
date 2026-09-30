@echo off
title CWLS - Collaborative Work Logging System
echo ========================================================
echo Starting Collaborative Work Logging System (CWLS)...
echo ========================================================

set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "build\libs\cwls.war" (
    echo [CWLS] Building application war file...
    call gradlew.bat bootWar -x test
)

echo [CWLS] Starting server on port 8080...
echo [CWLS] Open your browser at: http://localhost:8080
echo ========================================================
"%JAVA_HOME%\bin\java.exe" -jar build\libs\cwls.war
pause
