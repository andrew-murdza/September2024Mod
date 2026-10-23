[CmdletBinding()]
param(
    [string]$VrJar,
    [string]$HelperJar,
    [string]$RuntimeRoot,
    [string]$RunId,
    [string]$PreparedManifest,
    [int]$WarmupSeconds,
    [int]$BaselineSeconds,
    [int]$MeasureSeconds,
    [int]$SoakSeconds,
    [string]$Populations,
    [switch]$PrepareOnly
)

$ErrorActionPreference = 'Stop'
$prepareScript = Join-Path $PSScriptRoot 'prepare-benchmark.py'
if ($PreparedManifest) {
    if ($PrepareOnly) { throw 'Use either -PreparedManifest or -PrepareOnly.' }
    $manifestPath = (Resolve-Path -LiteralPath $PreparedManifest).Path
} else {
    $prepareArguments = @($prepareScript)
    if ($VrJar) { $prepareArguments += @('--vr-jar', $VrJar) }
    if ($HelperJar) { $prepareArguments += @('--helper-jar', $HelperJar) }
    if ($RuntimeRoot) { $prepareArguments += @('--runtime-root', $RuntimeRoot) }
    if ($RunId) { $prepareArguments += @('--run-id', $RunId) }
    if ($PSBoundParameters.ContainsKey('WarmupSeconds')) { $prepareArguments += @('--warmup-seconds', "$WarmupSeconds") }
    if ($PSBoundParameters.ContainsKey('BaselineSeconds')) { $prepareArguments += @('--baseline-seconds', "$BaselineSeconds") }
    if ($PSBoundParameters.ContainsKey('MeasureSeconds')) { $prepareArguments += @('--measure-seconds', "$MeasureSeconds") }
    if ($PSBoundParameters.ContainsKey('SoakSeconds')) { $prepareArguments += @('--soak-seconds', "$SoakSeconds") }
    if ($Populations) { $prepareArguments += @('--populations', $Populations) }
    $preparedText = & python @prepareArguments
    if ($LASTEXITCODE -ne 0) { throw 'Benchmark preparation failed; no server was launched.' }
    $prepared = ($preparedText -join "`n") | ConvertFrom-Json
    $manifestPath = $prepared.manifest
    $preparedText | Write-Output
    if ($PrepareOnly) { return }
}

& python $prepareScript --verify $manifestPath
if ($LASTEXITCODE -ne 0) { throw 'Benchmark inventory validation failed; no server was launched.' }
$manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
$launchRecord = Join-Path $manifest.instance 'launch.json'
if (Test-Path -LiteralPath $launchRecord) {
    throw 'This prepared benchmark was already launched. Prepare a new instance to preserve repeatability.'
}
# The one @argfile token is quoted for spaces; JVM arguments are stored in the
# verified file, avoiding PowerShell command construction or nested @argfiles.
$javaArguments = '"@{0}"' -f $manifest.argfile
$process = Start-Process -FilePath $manifest.java.path -ArgumentList $javaArguments `
    -WorkingDirectory $manifest.instance -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput $manifest.stdout -RedirectStandardError $manifest.stderr
$record = [ordered]@{
    startedUtc = [DateTime]::UtcNow.ToString('o')
    processId = $process.Id
    status = 'running'
    manifest = $manifestPath
    report = $manifest.report
}
$record | ConvertTo-Json | Set-Content -LiteralPath $launchRecord -Encoding UTF8
Write-Output "Benchmark process $($process.Id); log $($manifest.stdout); report $($manifest.report)"
$process.WaitForExit()
$process.Refresh()
$record.status = 'exited'
$record.finishedUtc = [DateTime]::UtcNow.ToString('o')
$record.exitCode = $process.ExitCode
$record | ConvertTo-Json | Set-Content -LiteralPath $launchRecord -Encoding UTF8
if ($process.ExitCode -ne 0) { throw "Benchmark server exited with code $($process.ExitCode). Inspect $($manifest.stderr)." }
if (-not (Test-Path -LiteralPath $manifest.report)) { throw 'Server exited without producing the benchmark report.' }
$benchmarkReport = Get-Content -LiteralPath $manifest.report -Raw | ConvertFrom-Json
$record.reportStatus = $benchmarkReport.status
$record | ConvertTo-Json | Set-Content -LiteralPath $launchRecord -Encoding UTF8
if ($benchmarkReport.status -ne 'completed') {
    throw "Benchmark did not complete (status: $($benchmarkReport.status)); retained report: $($manifest.report)"
}
Write-Output "Benchmark report: $($manifest.report)"
