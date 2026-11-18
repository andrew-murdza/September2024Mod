[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$SourceWorld,
    [Parameter(Mandatory = $true)][string]$Views,
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
$galleryRoot = Join-Path $projectRoot 'build/followup-worldgen-visuals'
$buildRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'build')).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
$sourceWorld = [IO.Path]::GetFullPath($SourceWorld)
$viewsPath = [IO.Path]::GetFullPath($Views)
if (!$sourceWorld.StartsWith($buildRoot, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'SourceWorld must be a closed, disposable QA world under this project build directory.'
}
if (!(Test-Path -LiteralPath (Join-Path $sourceWorld 'level.dat'))) { throw 'The closed QA source world has no level.dat.' }
if (!(Test-Path -LiteralPath $viewsPath -PathType Leaf)) { throw 'The exported viewpoint JSON is missing.' }
$clientRoot = Join-Path $galleryRoot ("client-$Attempt")
$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'build/standalone-client/launch.json') -Raw | ConvertFrom-Json
if (Test-Path -LiteralPath $clientRoot) { throw 'Use a new attempt to preserve preceding evidence and configuration.' }
New-Item -ItemType Directory -Path $clientRoot -Force | Out-Null
$snapshot = Join-Path $clientRoot 'runtime'
Copy-Item -LiteralPath $manifest.runtimeSnapshot -Destination $snapshot -Recurse
# Copy only a closed disposable QA world. The original and all user profiles stay untouched.
New-Item -ItemType Directory -Path (Join-Path $clientRoot 'saves') -Force | Out-Null
Copy-Item -LiteralPath $sourceWorld -Destination (Join-Path $clientRoot 'saves/FollowupQA') -Recurse
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
$arguments.Add('-Daoemod.structureAudit.views=' + $viewsPath)
$arguments.Add('-Daoemod.structureAudit.report=' + (Join-Path $clientRoot 'followup-screenshot-report.json'))
$arguments.Add('-Daoemod.structureAudit.exit=true')
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
$arguments.Add('--quickPlaySingleplayer'); $arguments.Add('FollowupQA')
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
    & (Join-Path $PSScriptRoot 'wait-for-minecraft-idle.ps1')
    $clientProcess = Start-Process -FilePath ([string]$manifest.javaExecutable) -ArgumentList ('@"' + $argumentFile + '"') -WorkingDirectory $clientRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $clientRoot 'stdout.log') -RedirectStandardError (Join-Path $clientRoot 'stderr.log') -PassThru
    Write-Output "Client PID: $($clientProcess.Id), follow-up native views, isolated directory: $clientRoot"
} finally {
    foreach ($name in $oldEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $oldEnvironment[$name], 'Process') }
}
