<#
.SYNOPSIS
Packages the November 8 worldgen followup without touching the main release.
.DESCRIPTION
ScreenshotReport is the passed ClientStructureScreenshotAudit JSON (views[].path).
QaReportsDirectory is a curated directory of completed JSON/XML evidence; only its
top-level reports are considered. Existing output folders and ZIPs are never reused:
pass a new -Version value for another bundle. The optional palette sheet is copied,
never regenerated. Run this script only after QA and visual review are complete.
.EXAMPLE
.\scripts\package-worldgen-followup.ps1 -ScreenshotReport build/followup-worldgen-visuals/client-1/followup-screenshot-report.json -QaReportsDirectory tmp/nov8-worldgen-followup/final-evidence -IncludePaletteComparison
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$ScreenshotReport,
    [Parameter(Mandatory = $true)][string]$QaReportsDirectory,
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9._-]{0,31}$')][string]$Version,
    [switch]$IncludePaletteComparison
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$projectRoot = [IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot))
$rootPrefix = $projectRoot.TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
$utf8 = [Text.UTF8Encoding]::new($false)
$plan = [Collections.Generic.List[object]]::new()
$destinations = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)

function Resolve-ProjectPath([string]$Path, [string]$Base = $projectRoot) {
    if (![IO.Path]::IsPathRooted($Path)) { $Path = Join-Path $Base $Path }
    $full = [IO.Path]::GetFullPath($Path)
    if (!$full.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Input must remain inside this project: $full"
    }
    return $full
}
function Assert-File([string]$Path) {
    if (!(Test-Path -LiteralPath $Path -PathType Leaf)) { throw "Required file is missing: $Path" }
    $item = Get-Item -LiteralPath $Path
    while ($null -ne $item -and $item.FullName -ne $projectRoot) {
        if (($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -ne 0) {
            throw "Package inputs may not use symlinks or junctions: $Path"
        }
        $item = if ($item -is [IO.FileInfo]) { $item.Directory } else { $item.Parent }
        if ($null -eq $item) { break }
    }
}
function Assert-Png([string]$Path) {
    Assert-File $Path
    if ([IO.Path]::GetExtension($Path) -ine '.png') { throw "Expected an original PNG: $Path" }
    $stream = [IO.File]::OpenRead($Path)
    try {
        $header = [byte[]]::new(24)
        if ($stream.Read($header, 0, 24) -ne 24 -or
                [BitConverter]::ToString($header, 0, 8) -ne '89-50-4E-47-0D-0A-1A-0A' -or
                [Text.Encoding]::ASCII.GetString($header, 12, 4) -ne 'IHDR') {
            throw "Invalid PNG header: $Path"
        }
    } finally { $stream.Dispose() }
}
function Add-Copy([string]$Source, [string]$Relative, [string]$Kind) {
    Assert-File $Source
    if ($Relative -match '(^|[\\/])\.\.([\\/]|$)' -or [IO.Path]::IsPathRooted($Relative)) {
        throw "Invalid package destination: $Relative"
    }
    if (!$destinations.Add($Relative)) { throw "Duplicate package destination: $Relative" }
    $plan.Add([pscustomobject]@{ Source = $Source; Relative = $Relative.Replace('\', '/'); Kind = $Kind })
}
function Html([string]$Value) { return [Net.WebUtility]::HtmlEncode($Value) }
function Read-Json([string]$Path) {
    Assert-File $Path
    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
}
function Property($Object, [string]$Name) {
    $entry = $Object.PSObject.Properties[$Name]
    if ($null -ne $entry) { return $entry.Value }
    return $null
}
function Scene-Category([string]$Label) {
    if ($Label -match '^(tropical_coast_|snowy_dappled_boundary_|desert_curved_mouth_)') { return 'Natural terrain' }
    if ($Label -match '^(cotton_stage_[0-3]|wild_cotton|pitcher_crop_stage_[1-4])$') { return 'Staged crop beds' }
    if ($Label -match '^(orc|spider|wither|yeti|mansion)[_-]') { return 'Staged structure fixtures' }
    throw "Unclassified screenshot label '$Label'; classify its actual scene before packaging."
}

# Validate everything before creating a release directory.
$reportPath = Resolve-ProjectPath $ScreenshotReport
$report = Read-Json $reportPath
if ((Property $report 'status') -cne 'passed') { throw 'ScreenshotReport must have status="passed".' }
$views = @(Property $report 'views')
if ($views.Count -eq 0 -or $null -eq $views[0]) { throw 'ScreenshotReport has no captured views.' }
if ((Property $report 'requestedViews') -ne $views.Count -or (Property $report 'completedViews') -ne $views.Count) {
    throw 'ScreenshotReport must contain every requested and completed view.'
}
if (Property $report 'failure') { throw 'ScreenshotReport still contains a failure.' }
$gallery = [Collections.Generic.List[object]]::new()
foreach ($view in $views) {
    $label = [string](Property $view 'label')
    if ($label -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,95}$') { throw "Unsafe screenshot label: $label" }
    $source = Resolve-ProjectPath ([string](Property $view 'path')) (Split-Path -Parent $reportPath)
    Assert-Png $source
    $category = Scene-Category $label
    $relative = "screenshots/$label.png"
    Add-Copy $source $relative $category
    $gallery.Add([pscustomobject]@{ Label = $label; Category = $category; Path = $relative })
}
Add-Copy $reportPath 'reports/screenshot-report.json' 'Passed screenshot capture report'

$followupRoot = Join-Path $projectRoot 'tmp/nov8-worldgen-followup'
$plantReportPath = Join-Path $followupRoot 'plant-recolor-report.json'
$plants = @(Read-Json $plantReportPath)
if ($plants.Count -ne 10) { throw 'The followup must contain exactly ten changed plant textures.' }
$textureNames = [Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($plant in $plants) {
    $texture = [string]$plant.texture
    if ($texture -notmatch '^(hearthandharvest/textures/block/(cotton_stage[0-3]|wild_cotton)|minecraft/textures/block/pitcher_crop_(bottom_stage_[1-4]|top_stage_3))\.png$' -or
            !$textureNames.Add($texture) -or $plant.alphaAndOtherColorsUnchanged -ne $true -or $plant.changedGreenPixels -le 0) {
        throw "Unexpected or unverified plant texture: $texture"
    }
    $before = Resolve-ProjectPath "plant-inputs/assets/$texture" $followupRoot
    $after = Resolve-ProjectPath "src/main/resources/assets/$texture"
    Assert-Png $before; Assert-Png $after
    if ((Get-FileHash -LiteralPath $after -Algorithm SHA256).Hash -ine [string]$plant.sha256) {
        throw "Plant texture differs from its passed recolor evidence: $texture"
    }
    Add-Copy $before "textures/before/assets/$texture" 'Original plant texture'
    Add-Copy $after "textures/after/assets/$texture" 'Recolored plant texture'
}
Add-Copy $plantReportPath 'reports/plant-recolor-report.json' 'Passed pixel-preservation evidence'
$templatePath = Resolve-ProjectPath 'src/main/resources/data/aoemod/structures/orc_settlement.nbt'
$templateAuditPath = Join-Path $followupRoot 'orc-settlement-template-audit.json'
$templateAudit = Read-Json $templateAuditPath
Assert-File $templatePath
if ((Get-FileHash -LiteralPath $templatePath -Algorithm SHA256).Hash -ine [string]$templateAudit.sha256) {
    throw 'The Orc settlement differs from its template audit.'
}
Add-Copy $templatePath 'structures/data/aoemod/structures/orc_settlement.nbt' 'Orc settlement template'
Add-Copy $templateAuditPath 'reports/orc-settlement-template-audit.json' 'Template evidence'
foreach ($name in @('generate-orc-settlement.py', 'generate-orc-camp-overrides.py', 'recolor-cotton-pitcher.ps1', 'package-worldgen-followup.ps1')) {
    Add-Copy (Join-Path $PSScriptRoot $name) "scripts/$name" 'Source script'
}

$qaRoot = Resolve-ProjectPath $QaReportsDirectory
if (!(Test-Path -LiteralPath $qaRoot -PathType Container)) { throw 'QaReportsDirectory does not exist.' }
$qaCount = 0
foreach ($file in Get-ChildItem -LiteralPath $qaRoot -File | Sort-Object Name) {
    if ($file.Extension -ieq '.json') {
        $evidence = Read-Json $file.FullName
        $status = Property $evidence 'status'
        $passedFlag = Property $evidence 'passed'
        if ($null -ne $status) {
            if ($status -notin @('passed', 'evidence')) { throw "QA report is unfinished or failed: $($file.Name) ($status)" }
        } elseif ($passedFlag -is [bool]) {
            if (!$passedFlag) { throw "QA report failed: $($file.Name)" }
            $status = 'passed'
        } elseif ($file.Name -notmatch '^(boss-density|boss-expansion-(metrics|views)|followup-worldgen-(views|scenes)|[a-z0-9_-]+-(audit|metrics|evidence))\.json$') {
            continue
        }
        Add-Copy $file.FullName "reports/qa/$($file.Name)" $(if ($status -eq 'passed') { 'Passed QA report' } else { 'Measured QA evidence' })
        $qaCount++
    } elseif ($file.Name -match '^(game-test-results|gametest-results|test-results)\.xml$') {
        $settings = [Xml.XmlReaderSettings]::new(); $settings.DtdProcessing = [Xml.DtdProcessing]::Prohibit
        $reader = [Xml.XmlReader]::Create($file.FullName, $settings)
        try { $xml = [Xml.XmlDocument]::new(); $xml.XmlResolver = $null; $xml.Load($reader) } finally { $reader.Dispose() }
        if ($xml.SelectNodes('//testcase').Count -eq 0 -or $xml.SelectNodes('//failure|//error').Count -ne 0) {
            throw "GameTest evidence is empty or contains failures: $($file.Name)"
        }
        Add-Copy $file.FullName "reports/qa/$($file.Name)" 'Passed GameTest report'
        $qaCount++
    }
}
if ($qaCount -eq 0) { throw 'QaReportsDirectory contains no completed QA reports or recognized measurement evidence.' }
if ($IncludePaletteComparison) {
    $palette = Join-Path $followupRoot 'plant-palette-comparison.png'
    Assert-Png $palette
    Add-Copy $palette 'plant-palette-comparison.png' 'Optional enlarged palette comparison'
}

$bundleName = 'worldgen-followup-2026-11-08'
if ($Version) { $bundleName += '-v' + $Version }
$bundle = Resolve-ProjectPath ("release/$bundleName")
$zipPath = "$bundle.zip"
$temporaryZip = "$zipPath.partial"
foreach ($target in @($bundle, $zipPath, $temporaryZip)) {
    if (Test-Path -LiteralPath $target) { throw "Output already exists: $target. Use a new explicit -Version value; nothing is overwritten." }
}

$html = [Text.StringBuilder]::new()
[void]$html.AppendLine('<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>Worldgen followup — 8 November 2026</title>')
[void]$html.AppendLine('<style>body{max-width:1100px;margin:2rem auto;padding:0 1rem;font:16px/1.5 system-ui;background:#fafafa;color:#172026}a{color:#075aa5}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));gap:1rem}figure{margin:0;padding:1rem;background:white;border:1px solid #ddd}img{display:block;max-width:100%;height:auto;margin:auto}figcaption{margin-top:.5rem}.pixel{width:96px;image-rendering:pixelated}table{border-collapse:collapse}td,th{padding:.5rem;border:1px solid #ccc;text-align:left}</style>')
[void]$html.AppendLine('<h1>Worldgen followup</h1><p>Original Minecraft screenshots and ten plant texture pairs. Every image links to its full original PNG; packaging does not crop, recolor or resample images.</p>')
foreach ($category in @('Natural terrain', 'Staged structure fixtures', 'Staged crop beds')) {
    $items = @($gallery | Where-Object Category -eq $category)
    if ($items.Count -eq 0) { continue }
    [void]$html.AppendLine("<h2>$(Html $category)</h2>")
    $description = switch ($category) {
        'Natural terrain' { 'Fresh naturally generated terrain, photographed in the game.' }
        'Staged structure fixtures' { 'Structures deliberately placed in QA fixtures to inspect their geometry, interiors and loot. These images do not demonstrate natural placement frequency.' }
        'Staged crop beds' { 'Native crop blocks deliberately placed on valid soil to show their exact growth stages. These are texture demonstrations, not natural crop generation.' }
    }
    [void]$html.AppendLine("<p>$description</p><div class=`"grid`">")
    foreach ($item in $items) {
        $label = Html ($item.Label.Replace('_', ' ').Replace('-', ' ')); $link = Html $item.Path
        [void]$html.AppendLine("<figure><a href=`"$link`"><img src=`"$link`" alt=`"$label`" loading=`"lazy`"></a><figcaption>$label · <a href=`"$link`">Full original PNG</a></figcaption></figure>")
    }
    [void]$html.AppendLine('</div>')
}
[void]$html.AppendLine('<h2>Plant textures: before and after</h2><p>Ten edited textures, each supplied as its original-resolution PNG. The before files are upstream originals; the after files match the pixel-preservation report.</p><table><tr><th>Texture</th><th>Before</th><th>After</th></tr>')
foreach ($plant in $plants) {
    $name = Html $plant.texture
    $before = Html "textures/before/assets/$($plant.texture)"; $after = Html "textures/after/assets/$($plant.texture)"
    [void]$html.AppendLine("<tr><td>$name</td><td><a href=`"$before`"><img class=`"pixel`" src=`"$before`" alt=`"Original $name`">Original PNG</a></td><td><a href=`"$after`"><img class=`"pixel`" src=`"$after`" alt=`"Recolored $name`">Edited PNG</a></td></tr>")
}
[void]$html.AppendLine('</table>')
if ($IncludePaletteComparison) { [void]$html.AppendLine('<p><a href="plant-palette-comparison.png">Optional enlarged palette comparison</a> — a separate comparison sheet; the raw textures above remain unchanged.</p>') }
[void]$html.AppendLine('<h2>Template, scripts and evidence</h2><p>This is a review/source bundle, not an installable mod or resource pack. Generator and recolor scripts are source copies for use in the original project with its pinned dependencies. Capture success records image creation; visual-review reports, when supplied, separately document inspection.</p><ul>')
foreach ($item in $plan | Where-Object { $_.Relative -match '^(structures|scripts|reports)/' } | Sort-Object Relative) {
    [void]$html.AppendLine("<li><a href=`"$(Html $item.Relative)`">$(Html $item.Relative)</a> — $(Html $item.Kind)</li>")
}
[void]$html.AppendLine('</ul></html>')

[IO.Directory]::CreateDirectory($bundle) | Out-Null
$manifest = [Collections.Generic.List[object]]::new()
foreach ($item in $plan | Sort-Object Relative) {
    $destination = Join-Path $bundle $item.Relative
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    [IO.File]::Copy($item.Source, $destination, $false)
    $sourceHash = (Get-FileHash -LiteralPath $item.Source -Algorithm SHA256).Hash.ToLowerInvariant()
    if ((Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant() -ne $sourceHash) {
        throw "Copy verification failed: $($item.Relative)"
    }
    $manifest.Add([ordered]@{ path = $item.Relative; kind = $item.Kind; sha256 = $sourceHash })
}
[IO.File]::WriteAllText((Join-Path $bundle 'index.html'), $html.ToString(), $utf8)
$metadata = [ordered]@{ bundle = $bundleName; texturePairs = 10; screenshots = $gallery.Count; qaReports = $qaCount; files = $manifest }
[IO.File]::WriteAllText((Join-Path $bundle 'package-manifest.json'), ($metadata | ConvertTo-Json -Depth 8) + "`n", $utf8)

# Stable entry order and timestamps make repeated packaging of identical inputs
# reproducible. No existing release archive, JAR or global checksum is opened.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zipStream = [IO.File]::Open($temporaryZip, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
try {
    $archive = [IO.Compression.ZipArchive]::new($zipStream, [IO.Compression.ZipArchiveMode]::Create, $true)
    try {
        foreach ($file in Get-ChildItem -LiteralPath $bundle -Recurse -File | Sort-Object FullName) {
            $relative = $file.FullName.Substring($bundle.Length + 1).Replace('\', '/')
            $entry = $archive.CreateEntry("$bundleName/$relative", [IO.Compression.CompressionLevel]::Optimal)
            $entry.LastWriteTime = [DateTimeOffset]::new(2026, 11, 8, 0, 0, 0, [TimeSpan]::Zero)
            $input = [IO.File]::OpenRead($file.FullName); $output = $entry.Open()
            try { $input.CopyTo($output) } finally { $output.Dispose(); $input.Dispose() }
        }
    } finally { $archive.Dispose() }
} finally { $zipStream.Dispose() }
Move-Item -LiteralPath $temporaryZip -Destination $zipPath
Write-Output "Created $bundle and $zipPath ($($gallery.Count) screenshots; ten raw texture pairs)."
