param([ValidateRange(0, 600)][int]$QuietSeconds = 120)
$ErrorActionPreference = 'Stop'
$previousIds = ''
$idleSince = $null
while ($true) {
    $games = @(Get-CimInstance Win32_Process | Where-Object {
        $_.Name -match '^java(w)?\.exe$' -and $_.CommandLine -and
        $_.CommandLine -match 'client\.args|BootstrapLauncher|forgegametestserver|net\.minecraft\.client\.main\.Main|KnotClient|KnotServer'
    })
    if (!$games.Count) {
        if ($null -eq $idleSince) {
            $idleSince = [DateTime]::UtcNow
            if ($QuietSeconds -gt 0) { Write-Output "Minecraft is closed; checking for a quiet $QuietSeconds-second launch interval." }
        }
        if (([DateTime]::UtcNow - $idleSince).TotalSeconds -ge $QuietSeconds) { break }
        Start-Sleep -Seconds 5
        continue
    }
    $idleSince = $null
    $ids = ($games.ProcessId | Sort-Object) -join ', '
    if ($ids -ne $previousIds) {
        Write-Output "Waiting for the existing Minecraft runtime(s) to finish: $ids"
        $previousIds = $ids
    }
    Start-Sleep -Seconds 15
}
