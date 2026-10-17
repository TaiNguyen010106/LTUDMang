$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir

& (Join-Path $projectDir "mvnw.cmd") clean test package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "Build thanh cong: target\npt-browser-1.3.0-all.jar"
