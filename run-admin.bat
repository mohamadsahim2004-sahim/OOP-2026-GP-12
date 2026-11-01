@echo off
rem Builds and starts the Faculty AMS with the MySQL driver on the classpath.
cd /d "%~dp0"
call mvn -q compile dependency:copy-dependencies -DoutputDirectory=target/lib
if errorlevel 1 (
  echo Build failed - fix the errors above, then run this file again.
  pause
  exit /b 1
)
java -cp "target/classes;target/lib/*" com.facultyams.Main
