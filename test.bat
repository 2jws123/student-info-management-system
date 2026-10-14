@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
title SIMS - run all unit tests

rem ====================================================================
rem  Test script: runs all 151 JUnit test cases and builds an HTML report
rem  Usage: double click this file.
rem ====================================================================

cd /d "%~dp0"

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
    pause
    exit /b 1
)

echo ======================================================
echo   Running all JUnit test cases (151 in total)
echo ======================================================
echo.
call "%MVN_CMD%" -B test
set "RESULT=%ERRORLEVEL%"

echo.
echo ======================================================
echo   Generating HTML report (target\site\surefire-report.html)
echo ======================================================
call "%MVN_CMD%" -B surefire-report:report-only >nul 2>&1
if exist "target\site\surefire-report.html" (
    echo Report: target\site\surefire-report.html
    start "" "target\site\surefire-report.html"
) else (
    echo [WARN] HTML report not generated. See target\surefire-reports for text reports.
)

echo.
if "%RESULT%"=="0" (
    echo [OK] All test cases passed.
) else (
    echo [FAILED] Some test cases failed. See target\surefire-reports
)
pause
endlocal
