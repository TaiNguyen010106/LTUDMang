$ErrorActionPreference = "Stop"
$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir

$builtJar = Join-Path $projectDir "target\npt-browser-1.3.0-all.jar"
$releaseJar = Join-Path $projectDir "release\npt-browser-1.3.0-all.jar"

if (Test-Path -LiteralPath $builtJar) {
    $jar = $builtJar
} elseif (Test-Path -LiteralPath $releaseJar) {
    $jar = $releaseJar
} else {
    & (Join-Path $projectDir "mvnw.cmd") clean package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $jar = $builtJar
}

$configArgument = "-Dnpt.config=config/application.properties"
& java "--enable-native-access=ALL-UNNAMED" "-Dfile.encoding=UTF-8" $configArgument -jar $jar
