[CmdletBinding()]
param(
    [ValidateRange(4, 8)][int]$HeapGB = 6,
    [ValidateRange(2, 8)][int]$ProcessorCount = 4,
    [switch]$PrepareOnly,
    [switch]$SkipPrepare,
    [switch]$Online
)

$ErrorActionPreference = 'Stop'
if ($SkipPrepare -and $Online) { throw '-Online cannot be combined with -SkipPrepare.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
$launchRoot = Join-Path $projectRoot 'build/standalone-client'
$manifestPath = Join-Path $launchRoot 'launch.json'

function Get-ProjectMinecraft {
    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^javaw?\.exe$' -and $_.CommandLine -and
        $_.CommandLine.IndexOf($projectRoot, [StringComparison]::OrdinalIgnoreCase) -ge 0 -and
        ($_.CommandLine -match 'standalone-client[\\/]client\.args' -or
            ($_.CommandLine -match 'BootstrapLauncher' -and
                $_.CommandLine -match '--launchTarget["\s=]+forgeclient(?:userdev|dev)?(?:["\s]|$)'))
    }
}

function ConvertTo-JavaArgument([string]$Value) {
    if ($Value.Contains([char]0)) { throw 'Java arguments cannot contain a null character.' }
    '"' + $Value.Replace('\', '\\').Replace('"', '\"').Replace("`r", '\r').Replace("`n", '\n') + '"'
}

# Serialize preparation and launch; a second click must not replace a live game's snapshot.
$projectHash = [System.Security.Cryptography.SHA256]::Create()
try { $mutexSuffix = [BitConverter]::ToString($projectHash.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot))).Replace('-', '').Substring(0, 16) }
finally { $projectHash.Dispose() }
$launcherMutex = New-Object System.Threading.Mutex($false, "Local\AOEModLauncher-$mutexSuffix")
$ownsMutex = $false
try {
    try { $ownsMutex = $launcherMutex.WaitOne(0) }
    catch [System.Threading.AbandonedMutexException] { $ownsMutex = $true }
    if (!$ownsMutex) { throw 'Another Minecraft launch is already being prepared.' }

    $existing = @(Get-ProjectMinecraft)
    if ($existing.Count -gt 0) {
        Write-Host "Minecraft is already running (PID $($existing[0].ProcessId))."
        return
    }

    New-Item -ItemType Directory -Force -Path $launchRoot | Out-Null
    if (!$SkipPrepare) {
        # Normal play uses the same installed dependencies as the verified build.
        # Still compile changed sources; only dependency resolution stays offline.
        # Use -Online explicitly when installing or updating dependencies.
        $preparationArguments = @('--no-daemon', '--max-workers=2', '-Dorg.gradle.jvmargs=-Xmx1536m',
            '-I', 'scripts/standalone-client.init.gradle', 'prepareStandaloneClient')
        if ($Online) {
            Write-Host 'Preparing the mod with online dependency resolution...'
        } else {
            $preparationArguments += '--offline'
            Write-Host 'Preparing the mod using installed dependencies. Gradle will exit before Minecraft starts...'
        }
        Push-Location $projectRoot
        try {
            & (Join-Path $projectRoot 'gradlew.bat') @preparationArguments
            if ($LASTEXITCODE -ne 0) {
                $hint = if ($Online) { '' } else { ' If dependencies are not installed, run scripts\launch-client.ps1 -Online once with an internet connection.' }
                throw "Minecraft preparation failed (exit $LASTEXITCODE).$hint"
            }
        } finally { Pop-Location }
    }
    if (!(Test-Path -LiteralPath $manifestPath)) { throw 'No prepared launch exists. Run without -SkipPrepare first.' }
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if ($manifest.schemaVersion -ne 1) { throw 'Unsupported launch manifest; prepare the client again.' }
    if ($PrepareOnly) {
        Write-Host "Prepared standalone launcher: $manifestPath"
        return
    }
    if (@(Get-ProjectMinecraft).Count -gt 0) { throw 'Minecraft started during preparation; refusing a duplicate launch.' }

    $javaExecutable = [string]$manifest.javaExecutable
    $javaWindowless = Join-Path (Split-Path -Parent $javaExecutable) 'javaw.exe'
    if (Test-Path -LiteralPath $javaWindowless) { $javaExecutable = $javaWindowless }
    if (!(Test-Path -LiteralPath $javaExecutable)) { throw "Java is missing: $javaExecutable" }

    $javaArguments = New-Object 'System.Collections.Generic.List[string]'
    foreach ($argument in $manifest.jvmArgs) {
        if ($argument -notmatch '^-Xm[sx]' -and $argument -notmatch '^-XX:(ActiveProcessorCount|G1PeriodicGCInterval)=') {
            $javaArguments.Add([string]$argument)
        }
    }
    $javaArguments.Add('-Xms1G')
    $javaArguments.Add("-Xmx${HeapGB}G")
    $javaArguments.Add("-XX:ActiveProcessorCount=$ProcessorCount")
    # Concurrent periodic G1 collections can release unused heap after returning to the menu.
    $javaArguments.Add('-XX:G1PeriodicGCInterval=30000')
    $javaArguments.Add('-Xlog:gc=info,safepoint=warning:file=logs/standalone-client-gc.log:time,uptime,level,tags:filecount=3,filesize=8M')
    $javaArguments.Add('-cp')
    $javaArguments.Add([string]::Join([IO.Path]::PathSeparator, [string[]]$manifest.classpath))
    $javaArguments.Add([string]$manifest.mainClass)
    foreach ($argument in $manifest.programArgs) { $javaArguments.Add([string]$argument) }

    $argumentFile = Join-Path $launchRoot 'client.args'
    $utf8 = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllLines($argumentFile, [string[]]@($javaArguments | ForEach-Object { ConvertTo-JavaArgument $_ }), $utf8)
    New-Item -ItemType Directory -Force -Path (Join-Path $manifest.workingDirectory 'logs') | Out-Null

    $oldEnvironment = @{}
    try {
        foreach ($entry in $manifest.environment.PSObject.Properties) {
            $oldEnvironment[$entry.Name] = [Environment]::GetEnvironmentVariable($entry.Name, 'Process')
            [Environment]::SetEnvironmentVariable($entry.Name, [string]$entry.Value, 'Process')
        }
        # javaw creates the interactive game window without keeping a console/Gradle process.
        $clientProcess = Start-Process -FilePath $javaExecutable -ArgumentList ('@"' + $argumentFile + '"') -WorkingDirectory $manifest.workingDirectory -WindowStyle Normal -RedirectStandardOutput (Join-Path $launchRoot 'stdout.log') -RedirectStandardError (Join-Path $launchRoot 'stderr.log') -PassThru
    } finally {
        foreach ($name in $oldEnvironment.Keys) { [Environment]::SetEnvironmentVariable($name, $oldEnvironment[$name], 'Process') }
    }

    [ordered]@{ processId = $clientProcess.Id; startedAt = $clientProcess.StartTime.ToUniversalTime().ToString('o'); heapGB = $HeapGB; processorCount = $ProcessorCount; argumentsFile = $argumentFile } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $launchRoot 'running.json') -Encoding UTF8
    Start-Sleep -Seconds 2
    if ($clientProcess.HasExited) { throw "Minecraft exited during startup. See $launchRoot\stderr.log and stdout.log." }
    Write-Host "Minecraft started (PID $($clientProcess.Id)), with a ${HeapGB} GB Java heap limit."
    Write-Host 'The game also uses native/graphics memory. IntelliJ and Gradle are not needed while playing.'
} finally {
    if ($ownsMutex) { $launcherMutex.ReleaseMutex() }
    $launcherMutex.Dispose()
}
