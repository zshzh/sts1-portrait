[CmdletBinding()]
param(
    [string]$BaseTag = 'v1.6.7',
    [string]$Tag,
    [string]$Patch = (Join-Path $PSScriptRoot 'Amethyst-plus.patch'),
    [string]$Checkout,
    [string]$ApplyTo
)

$ErrorActionPreference = 'Stop'
$repository = 'https://github.com/ModinMobileSTS/SlayTheAmethystModded.git'
if (!$Tag) {
    $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/ModinMobileSTS/SlayTheAmethystModded/releases/latest'
    $Tag = $release.tag_name
}
if ($Tag -notmatch '^v[0-9]+(?:\.[0-9]+)+(?:-[a-zA-Z0-9][a-zA-Z0-9.-]*)?$') {
    throw "Unexpected release tag: $Tag"
}
Write-Host "Plus base: $BaseTag; upstream release: $Tag"
if (!$Checkout -and !$ApplyTo -and $Tag -eq $BaseTag) {
    Write-Host 'Already on the latest upstream release.'
    return
}
$resolvedPatch = (Resolve-Path -LiteralPath $Patch).Path
if ($ApplyTo -and $Checkout) {throw 'Specify either -Checkout or -ApplyTo.'}
if ($ApplyTo) {
    $Checkout = $ApplyTo
}
if (!$Checkout) {
    $checkouts = if ($env:RUNNER_TEMP) { $env:RUNNER_TEMP } else { [IO.Path]::GetTempPath() }
    $Checkout = Join-Path $checkouts ("amethyst-" + $Tag + "-" + [guid]::NewGuid().ToString('N'))
}
if (!(Test-Path -LiteralPath $Checkout)) {
    & git -c core.autocrlf=false clone --depth 1 --single-branch --branch $Tag $repository $Checkout
    if ($LASTEXITCODE) {throw 'Could not clone the release tag.'}
}
$Checkout = (Resolve-Path -LiteralPath $Checkout).Path
$expected = & git -C $Checkout rev-parse --verify ("refs/tags/" + $Tag + "^{commit}")
if ($LASTEXITCODE) {throw "Checkout lacks tag $Tag."}
$head = & git -C $Checkout rev-parse HEAD
if ($LASTEXITCODE -or $expected -ne $head) {throw "Checkout must be at release tag $Tag."}
$status = & git -C $Checkout status --porcelain
if ($LASTEXITCODE -or $status) {throw 'Use a clean checkout.'}
& git -C $Checkout apply --check $resolvedPatch
if ($LASTEXITCODE) {throw "Plus patch needs migration to $Tag. Checkout left unchanged."}
if ($ApplyTo) {
    & git -C $Checkout apply $resolvedPatch
    if ($LASTEXITCODE) {throw "Could not apply Plus patch to $Tag."}
    Write-Host "Plus changes applied locally to $Tag in $Checkout."
} else {
    Write-Host "Plus patch applies to $Tag. Build and test next."
}
