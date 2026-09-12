# PowerShell script to compile and launch the Faculty of Technology Academic Management System
$ErrorActionPreference = "Stop"

$jdkBin = "C:\Program Files\Java\jdk-26.0.2.1\bin"
$javac = Join-Path $jdkBin "javac.exe"
$java  = Join-Path $jdkBin "java.exe"

if (!(Test-Path $javac)) {
    # Fallback to system path if custom path not found
    $javac = "javac"
    $java  = "java"
}

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Faculty of Technology Academic Management System (FOT-AMS)" -ForegroundColor Yellow
Write-Host " University of Ruhuna • Course Unit: ICT2132" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan

if (!(Test-Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

Write-Host "Compiling Java sources..." -ForegroundColor Cyan
$sources = Get-ChildItem -Path "src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& $javac -encoding UTF-8 -d "bin" -sourcepath "src" $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilation successful! Starting application..." -ForegroundColor Green
    & $java -cp "bin" com.fot.ams.Main
} else {
    Write-Host "Compilation failed with code $LASTEXITCODE" -ForegroundColor Red
}
