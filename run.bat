@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title SIMS - Student Information Management System

rem ====================================================================
rem  Student Information Management System - launcher
rem  Usage: double click this file. Optional args are passed to the app,
rem         e.g. "run.bat --screenshot" or "run.bat --help".
rem  Build first with build.bat if target\sims.jar does not exist.
rem ====================================================================

cd /d "%~dp0"

set "JAR=target\sims.jar"
if not exist "%JAR%" (
    echo [ERROR] %JAR% not found. Please run build.bat first.
    pause
    exit /b 1
)

rem ---------- 1) portable JDK shipped next to the project ----------
set "JAVA_EXE="
for /d %%D in ("%~dp0..\.tools\jdk\jdk-*") do (
    if exist "%%D\bin\java.exe" set "JAVA_EXE=%%D\bin\java.exe"
)

rem ---------- 2) JAVA_HOME ----------
if not defined JAVA_EXE (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

rem ---------- 3) java on PATH ----------
if not defined JAVA_EXE (
    for /f "delims=" %%J in ('where java 2^>nul') do (
        if not defined JAVA_EXE set "JAVA_EXE=%%J"
    )
)

if not defined JAVA_EXE (
    echo [ERROR] No JDK found. Java 17 or later is required.
    echo         Option 1: unzip a JDK into ..\.tools\jdk
    echo         Option 2: set the JAVA_HOME environment variable
    echo         Option 3: open this project in IntelliJ IDEA and run com.course.sims.App
    pause
    exit /b 1
)

echo ======================================================
echo   SIMS v1.0.0  -  Student Information Management System
echo   JDK: %JAVA_EXE%
echo   Accounts: admin / teacher / zs  password 123456
echo             t2024001              password 2024001
echo ======================================================
echo.

"%JAVA_EXE%" -Dfile.encoding=UTF-8 -Dsun.java2d.uiScale=1.0 -jar "%JAR%" %*
set "EXITCODE=%ERRORLEVEL%"

if not "%EXITCODE%"=="0" (
    echo.
    echo [WARN] exit code = %EXITCODE%
    pause
)
endlocal
