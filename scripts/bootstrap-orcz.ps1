param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$libraryPath = Join-Path $projectRoot 'libs/pack/orcz-0.87-1.20.1.jar'
$expectedHash = '6C57D5A22FA34B06D3B5CB72EA8E11DADAC611527CD54610AF0F44E42D01908B'

# Official author's Forge 1.20.1 release: curseforge.com/minecraft/mc-mods/orcz/files/4816086.
# Keep the original binary local; the dependency manifests record its exact provenance.
if (!(Test-Path -LiteralPath $libraryPath)) {
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $libraryPath) | Out-Null
    Invoke-WebRequest -Uri 'https://www.cursemaven.com/curse/maven/orcz-581652/4816086/orcz-581652-4816086.jar' -OutFile $libraryPath
}
if ((Get-FileHash -LiteralPath $libraryPath -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'The local Orcz archive does not match the pinned official release.'
}
Write-Output 'Verified Orcz 0.87 for Forge 1.20.1.'
