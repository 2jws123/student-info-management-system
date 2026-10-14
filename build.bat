@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title SIMS - build

rem ====================================================================
rem  Build script: clean package  ->  target\sims.jar
rem  Usage: double click this file.
rem ====================================================================

cd /d "%~dp0"

rem ---------- locate JDK ----------
set "JDK_DIR="
for /d %%D in ("%~dp0..\.tools\jdk\jdk-*") do (
    if exist "%%D\bin\javac.exe" set "JDK_DIR=%%D"
)
if not defined JDK_DIR (
    if exist "%JAVA_HOME%\bin\javac.exe" set "JDK_DIR=%JAVA_HOME%"
)
if not defined JDK_DIR (
    echo [ERROR] No JDK found. Set JAVA_HOME or unzip a JDK into ..\.tools\jdk
    pause
    exit /b 1
)
set "JAVA_HOME=%JDK_DIR%"
echo [1/3] JDK   : %JAVA_HOME%

rem ---------- locate Maven ----------
set "MVN_CMD="
for /d %%D in ("%~dp0..\.tools\maven\apache-maven-*") do (
    if exist "%%D\bin\mvn.cmd" set "MVN_CMD=%%D\bin\mvn.cmd"
)
if not defined MVN_CMD (
    for /f "delims=" %%M in ('where mvn 2^>nul') do (
        if not defined MVN_CMD set "MVN_CMD=%%M"
    )
)
if not defined MVN_CMD (
    echo [ERROR] Maven not found.
    echo         Option 1: unzip Maven into ..\.tools\maven
    echo         Option 2: add mvn to PATH
    echo         Option 3: use IntelliJ IDEA Maven panel to run package
    pause
    exit /b 1
)
echo [2/3] Maven : %MVN_CMD%

echo [3/3] Running "mvn clean package" ...
echo       First run downloads dependencies from Maven Central, please wait.
echo.
call "%MVN_CMD%" -B clean package
if errorlevel 1 (
    echo.
    echo [FAILED] Build failed, see the messages above.
    pause
    exit /b 1
)

echo.
echo [OK] target\sims.jar generated. Double click run.bat to start the system.
pause
endlocal
