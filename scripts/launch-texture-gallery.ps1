[CmdletBinding()]
param(
    [ValidateSet('after', 'before', 'portal')][string]$Mode = 'after',
    [ValidatePattern('^[a-zA-Z0-9-]+$')][string]$Attempt = '1'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$existingMinecraft = @(Get-CimInstance Win32_Process | Where-Object {
    $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -and
    $_.CommandLine -match 'client\.args|BootstrapLauncher|net\.minecraft\.client\.main\.Main|net\.fabricmc\.loader\..*Knot(Client|Server)'
})
if ($existingMinecraft.Count -gt 0) {
    throw 'A Minecraft client or test server is already running. Close it before starting another QA instance.'
}
$galleryRoot = Join-Path $projectRoot 'build/texture-gallery'
$clientRoot = Join-Path $galleryRoot ("client-$Mode-$Attempt")
$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'build/standalone-client/launch.json') -Raw | ConvertFrom-Json
if (Test-Path -LiteralPath $clientRoot) { throw 'Use a new attempt to preserve preceding evidence and configuration.' }
New-Item -ItemType Directory -Path $clientRoot -Force | Out-Null
$snapshot = Join-Path $clientRoot 'runtime'
Copy-Item -LiteralPath $manifest.runtimeSnapshot -Destination $snapshot -Recurse
if ($Mode -eq 'before') {
    $overrides = Join-Path $galleryRoot 'before-overrides/assets'
    foreach ($path in Get-ChildItem -LiteralPath $overrides -Recurse -File) {
        $relative = $path.FullName.Substring($overrides.Length + 1)
        $destination = Join-Path (Join-Path $snapshot 'assets') $relative
        New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
        Copy-Item -LiteralPath $path.FullName -Destination $destination
    }
}
# Use a closed, disposable QA world. Never launch or change the user's play world.
$sourceWorld = Join-Path $projectRoot 'build/nov7-expansion-client-3/saves/ExpansionQA'
if (!(Test-Path -LiteralPath (Join-Path $sourceWorld 'level.dat'))) { throw 'The closed ExpansionQA source world is missing.' }
New-Item -ItemType Directory -Path (Join-Path $clientRoot 'saves') -Force | Out-Null
Copy-Item -LiteralPath $sourceWorld -Destination (Join-Path $clientRoot 'saves/GalleryQA') -Recurse
# Existing mod settings are copied into the isolated run, never replaced in run/config.
if (Test-Path -LiteralPath (Join-Path $projectRoot 'run/config')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot 'run/config') -Destination (Join-Path $clientRoot 'config') -Recurse
}
[IO.File]::WriteAllText((Join-Path $clientRoot 'options.txt'), "renderDistance:3`nsimulationDistance:5`nsoundCategory_master:0.0`nfullscreen:false`npauseOnLostFocus:false`nmaxFps:60`nguiScale:2`n", [Text.UTF8Encoding]::new($false))
function ConvertTo-JavaArgument([string]$Value) {
    if ($Value.Contains([char]0)) { throw 'Invalid Java argument' }
    '"' + $Value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '\r').Replace("`n", '\n') + '"'
}
$arguments = [Collections.Generic.List[string]]::new()
foreach ($argument in $manifest.jvmArgs) {
    if ($argument -notmatch '^-Xm[sx]' -and $argument -notmatch '^-XX:ActiveProcessorCount=' -and $argument -notmatch '^-Daoemod\.(gallery|assetAudit|structureAudit|portalAudit)\.') {
        $arguments.Add([string]$argument)
    }
}
if ($Mode -eq 'portal') {
    $arguments.Add('-Daoemod.portalAudit.report=' + (Join-Path $galleryRoot "portal-$Attempt.json"))
    $arguments.Add('-Daoemod.portalAudit.exit=true')
} else {
    $arguments.Add('-Daoemod.gallery.views=' + (Join-Path $galleryRoot "$Mode-views.json"))
    $arguments.Add('-Daoemod.gallery.output=' + (Join-Path $galleryRoot "screenshots/$Mode"))
    $arguments.Add('-Daoemod.gallery.exit=true')
    $arguments.Add('-Daoemod.gallery.derivedTextures=' + (Join-Path $galleryRoot 'derived-textures'))
    if ($Mode -eq 'before') {
        $arguments.Add('-Daoemod.gallery.before=true')
        $arguments.Add('-Daoemod.gallery.originalTextures=' + (Join-Path $galleryRoot 'before-overrides/assets'))
    }
}
$arguments.Add('-Xms1G'); $arguments.Add('-Xmx5G'); $arguments.Add('-XX:ActiveProcessorCount=2')
$arguments.Add('-cp')
$classpath = @($manifest.classpath | ForEach-Object { if ($_ -eq $manifest.runtimeSnapshot) { $snapshot } else { [string]$_ } })
$arguments.Add([string]::Join([IO.Path]::PathSeparator, [string[]]$classpath))
$arguments.Add([string]$manifest.mainClass)
for ($i = 0; $i -lt $manifest.programArgs.Count; $i++) {
    $argument = [string]$manifest.programArgs[$i]
    $arguments.Add($argument)
    if ($argument -eq '--gameDir') { $arguments.Add($clientRoot); $i++ }
    elseif ($argument -eq '--width') { $arguments.Add('1440'); $i++ }
    elseif ($argument -eq '--height') { $arguments.Add('1000'); $i++ }
}
if ($manifest.programArgs -notcontains '--width') { $arguments.Add('--width'); $arguments.Add('1440') }
if ($manifest.programArgs -notcontains '--height') { $arguments.Add('--height'); $arguments.Add('1000') }
$arguments.Add('--quickPlaySingleplayer'); $arguments.Add('GalleryQA')
$argumentFile = Join-Path $clientRoot 'client.args'
[IO.File]::WriteAllLines($argumentFile, [string[]]@($arguments | ForEach-Object { ConvertTo-JavaArgument $_ }), [Text.UTF8Encoding]::new($false))
$oldEnvironment = @{}
try {
    foreach ($entry in $manifest.environment.PSObject.Properties) {
        $oldEnvironment[$entry.Name] = [Environment]::GetEnvironmentVariable($entry.Name, 'Process')
        [Environment]::SetEnvironmentVariable($entry.Name, [string]$entry.Value, 'Process')
    }
    $oldEnvironment['MOD_CLASSES'] = [Environment]::GetEnvironmentVariable('MOD_CLASSES', 'Process')
    [Environment]::SetEnvironmentVariable('MOD_CLASSES', 'aoemod%%' + $snapshot, 'Process')
    $clientProcess = Start-Process -FilePath ([string]$manifest.javaExecutable) -ArgumentList ('@"' + $argumentFile + '"') -WorkingDirectory $clientRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $clientRoot 'stdout.log') -RedirectStandardError (Join-Path $clientRoot 'stderr.log') -PassThru
    Write-Output "Client PID: $($clientProcess.Id), mode: $Mode, isolated directory: $clientRoot"
} finally {
    foreach ($name in $oldEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $oldEnvironment[$name], 'Process') }
}
