param()
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Join-Path $projectRoot 'build/future-backports'
$libraryRoot = Join-Path $projectRoot 'libs'
New-Item -ItemType Directory -Force -Path $sourceRoot, $libraryRoot | Out-Null

function Get-PinnedSource([string]$repository, [string]$revision) {
    $name = ($repository -split '/')[-1]
    $destination = Join-Path $sourceRoot "$name-$revision"
    if (!(Test-Path -LiteralPath $destination)) {
        $archive = Join-Path $sourceRoot "$name-$revision.zip"
        Invoke-WebRequest -Uri "https://codeload.github.com/$repository/zip/$revision" -OutFile $archive
        Expand-Archive -LiteralPath $archive -DestinationPath $sourceRoot
    }
    return $destination
}

function Invoke-DependencyBuild([string]$wrapper, [string]$directory, [string[]]$tasks) {
    & $wrapper -p $directory @tasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Dependency build failed in $directory" }
}

# Private source builds: VanillaBackport does not permit redistribution of rebuilt jars.
$platformSource = Get-PinnedSource 'ItsBlackGear/Platform' '73f4cddb2d8d60ec132a0d373a954bccd9bd1768'
$backportSource = Get-PinnedSource 'ItsBlackGear/VanillaBackport' 'c69c9f674ba91d3181b8ccdc90ae8dab9133ca39'
$wrapperJar = Join-Path $backportSource 'gradle/wrapper/gradle-wrapper.jar'
Copy-Item -LiteralPath (Join-Path $projectRoot 'gradle/wrapper/gradle-wrapper.jar') -Destination $wrapperJar -Force
$wrapper = Join-Path $backportSource 'gradlew.bat'
Invoke-DependencyBuild $wrapper $platformSource @(':common:remapJar', ':fabric:remapJar', ':forge:remapJar')

$backportLibraries = Join-Path $backportSource 'libs'
New-Item -ItemType Directory -Force -Path $backportLibraries | Out-Null
foreach ($loader in @('common', 'fabric', 'forge')) {
    $file = Join-Path $platformSource "$loader/build/libs/Platform-$loader-1.20.1-1.4.0.2640.1.jar"
    Copy-Item -LiteralPath $file -Destination $backportLibraries -Force
}
$backportBuild = Join-Path $backportSource 'build.gradle'
$buildText = [IO.File]::ReadAllText($backportBuild)
$buildText = $buildText.Replace('flatDir { dirs "/libs" }', 'flatDir { dirs rootProject.file("libs") }')
[IO.File]::WriteAllText($backportBuild, $buildText, [Text.UTF8Encoding]::new($false))
Invoke-DependencyBuild $wrapper $backportSource @(':forge:remapJar')
Copy-Item -LiteralPath (Join-Path $platformSource 'forge/build/libs/Platform-forge-1.20.1-1.4.0.2640.1.jar') -Destination $libraryRoot -Force
Copy-Item -LiteralPath (Join-Path $backportSource 'forge/build/libs/VanillaBackport-forge-1.20.1-1.2.0.2640.1.jar') -Destination $libraryRoot -Force

# Tiny Takeover is MIT-licensed. Adapt only the renamed Armadillo API and Dolphin overlap.
$tinyJar = Join-Path $sourceRoot 'tiny_takeover_backport-forge-1.20.1-1.6.3.jar'
Invoke-WebRequest -Uri 'https://cdn.modrinth.com/data/F4TmfqsY/versions/UXODXup2/tiny_takeover_backport-forge-1.20.1-1.6.3.jar' -OutFile $tinyJar
$expectedHash = 'fea1fce49b0c284ca37bc82eb21f1ca95b997d61577b666298e1c19820803673d00c698a1d86059dca385f8fb474c81576efce6657004791b255f8f20edbabd7'
if ((Get-FileHash -LiteralPath $tinyJar -Algorithm SHA512).Hash.ToLowerInvariant() -ne $expectedHash) {
    throw 'Tiny Takeover checksum mismatch; no compatibility transformation performed.'
}
$asmJar = Join-Path $sourceRoot 'asm-9.7.1.jar'
$asmCommonsJar = Join-Path $sourceRoot 'asm-commons-9.7.1.jar'
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm/9.7.1/asm-9.7.1.jar' -OutFile $asmJar
Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/ow2/asm/asm-commons/9.7.1/asm-commons-9.7.1.jar' -OutFile $asmCommonsJar
& java --class-path "$asmJar;$asmCommonsJar" (Join-Path $PSScriptRoot 'PatchTinyTakeover.java') $tinyJar (Join-Path $libraryRoot 'tiny_takeover_backport-forge-1.20.1-1.6.3-aoe.jar')
if ($LASTEXITCODE -ne 0) { throw 'Tiny Takeover compatibility transform failed.' }
Write-Host 'Private future-backport dependencies are ready in libs/. Run .\gradlew.bat build.'
