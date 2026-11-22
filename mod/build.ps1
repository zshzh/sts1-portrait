[CmdletBinding()]
param(
    [string]$JdkDirectory = $env:JAVA_HOME,
    [string]$FrameworkDirectory,
    [string]$GameJar
)

$ErrorActionPreference = 'Stop'

if (-not $FrameworkDirectory) {
    $FrameworkDirectory = Join-Path $PSScriptRoot '.local/framework'
}

if (-not $JdkDirectory) {
    $cachedJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.local/jdk') -Directory -ErrorAction SilentlyContinue |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin/javac.exe') -PathType Leaf } |
        Sort-Object Name -Descending | Select-Object -First 1
    if ($cachedJdk) {
        $JdkDirectory = $cachedJdk.FullName
    }
}

if ($JdkDirectory) {
    $javac = (Resolve-Path -LiteralPath (Join-Path $JdkDirectory 'bin/javac.exe')).Path
    $jar = (Resolve-Path -LiteralPath (Join-Path $JdkDirectory 'bin/jar.exe')).Path
} else {
    $javac = (Get-Command javac.exe -CommandType Application -ErrorAction Stop).Source
    $jar = (Resolve-Path -LiteralPath (Join-Path (Split-Path $javac) 'jar.exe')).Path
}

$javacVersion = & {
    # JDK 8 writes its version to stderr, which Windows PowerShell treats as an error record.
    $ErrorActionPreference = 'Continue'
    & $javac -version 2>&1
}
if ($LASTEXITCODE -ne 0 -or "$javacVersion" -notmatch '^javac 1\.8\.0') {
    throw "This build uses JDK 8. Select it with -JdkDirectory. Found: $javacVersion"
}
Write-Host $javacVersion

$classpath = foreach ($name in 'ModTheSpire.jar', 'BaseMod.jar') {
    $path = Join-Path $FrameworkDirectory $name
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) {
        throw "Missing dependency: $path. Specify -FrameworkDirectory with the launcher framework JARs."
    }
    (Resolve-Path -LiteralPath $path).Path
}
if ($GameJar) {
    if (-not (Test-Path -LiteralPath $GameJar -PathType Leaf)) {
        throw "Game JAR not found: $GameJar"
    }
    $classpath += (Resolve-Path -LiteralPath $GameJar).Path
} else {
    throw 'Specify -GameJar with the path to your Steam desktop-1.0.jar.'
}

$buildDirectory = Join-Path $PSScriptRoot 'build'
$stagingDirectory = Join-Path $buildDirectory ([guid]::NewGuid().ToString('N'))
$classes = Join-Path $stagingDirectory 'classes'
$artifact = Join-Path $buildDirectory 'STS1Portrait.jar'
$metadataDirectory = Join-Path $PSScriptRoot 'src/main/resources'
$sources = @(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src/main/java') -Recurse -File -Filter '*.java' |
    Sort-Object FullName | Select-Object -ExpandProperty FullName)

try {
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    & $javac -g -encoding UTF-8 -classpath ($classpath -join [IO.Path]::PathSeparator) -d $classes @sources
    if ($LASTEXITCODE -ne 0) { throw "javac exited with code $LASTEXITCODE" }
    $stagedJar = Join-Path $stagingDirectory 'STS1Portrait.jar'
    & $jar cf $stagedJar -C $classes . -C $metadataDirectory ModTheSpire.json
    if ($LASTEXITCODE -ne 0) { throw "jar exited with code $LASTEXITCODE" }
    Move-Item -LiteralPath $stagedJar -Destination $artifact -Force
    Write-Host "Built $artifact"
} finally {
    if (Test-Path -LiteralPath $stagingDirectory -PathType Container) {
        # The directory was created under build for this invocation only.
        $resolvedStage = (Resolve-Path -LiteralPath $stagingDirectory).Path
        $resolvedBuild = (Resolve-Path -LiteralPath $buildDirectory).Path
        if ((Split-Path $resolvedStage -Parent) -eq $resolvedBuild) {
            Remove-Item -LiteralPath $resolvedStage -Recurse -Force
        }
    }
}
