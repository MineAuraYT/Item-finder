$ErrorActionPreference = 'Stop'

Write-Host '=== ITEM FINDER MOD BUILD ===' -ForegroundColor Cyan

try {
    $javaOutput = cmd.exe /c "java -version 2>&1"
    if ($LASTEXITCODE -ne 0) { throw 'Java was not found. Install JDK 25 and add it to PATH.' }
    Write-Host ($javaOutput | Out-String)
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
}

$gradleCommand = Get-Command gradle -ErrorAction SilentlyContinue
if (-not $gradleCommand) {
    Write-Host 'Gradle was not found. Install Gradle 9.5.1 or newer and add it to PATH.' -ForegroundColor Red
    exit 1
}

if ($PSVersionTable.PSVersion.Major -ge 7) {
    $PSNativeCommandArgumentPassing = 'Legacy'
}

gradle --version
if ($LASTEXITCODE -ne 0) { throw 'Gradle could not start.' }

gradle clean build
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Build failed. Copy the complete error output and send it for diagnosis.' -ForegroundColor Red
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path '.\JAR' | Out-Null
Get-ChildItem '.\build\libs\*.jar' | Where-Object { $_.Name -notmatch 'sources|javadoc|dev' } | ForEach-Object {
    Copy-Item $_.FullName '.\JAR\' -Force
}
Write-Host 'Build complete. Check the JAR folder.' -ForegroundColor Green
