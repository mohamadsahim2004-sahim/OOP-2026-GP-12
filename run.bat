@echo off
title Faculty of Technology Academic Management System - University of Ruhuna
echo ==========================================================
echo  Faculty of Technology Academic Management System (FOT-AMS)
echo  University of Ruhuna - Course Unit: ICT2132
echo ==========================================================

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "bin" mkdir "bin"

echo Compiling Java source files...
dir /s /b "src\*.java" > sources.txt
javac -encoding UTF-8 -d "bin" -sourcepath "src" @sources.txt
del sources.txt

if %ERRORLEVEL% equ 0 (
    echo Starting Modern Login Application...
    start javaw -cp "bin" com.fot.ams.Main
) else (
    echo Compilation failed!
    pause
)
