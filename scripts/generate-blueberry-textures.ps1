$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

# Preserve the native leaf, snow, silhouette and transparency pixels; recolor
# only ripe berries using Fruits Delight's actual blueberry item palette.
$projectRoot = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $projectRoot 'src/main/resources/assets/aoemod'
$evidence = Join-Path $projectRoot 'tmp/blueberry-textures'
New-Item -ItemType Directory -Force -Path "$assets/textures/block", $evidence | Out-Null
$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'production-dependencies.json') -Raw | ConvertFrom-Json
$snow = $manifest.artifacts | Where-Object { $_.filename -like 'snow-real-magic-*' }
$sources = @(
    @{ Path = Join-Path $env:USERPROFILE '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'; Namespace = 'minecraft'; Prefix = '' },
    @{ Path = Join-Path $env:USERPROFILE ('.gradle/caches/modules-2/files-2.1/' + $snow.source.path); Namespace = 'snowrealmagic'; Prefix = 'snowy_' }
)
$berryArchive = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'libs/pack/fruitsdelight-1.1.3.jar'))
try {
    $stream = $berryArchive.GetEntry('assets/fruitsdelight/textures/item/blueberry.png').Open()
    $reference = [Drawing.Bitmap]::new($stream)
    try {
        $paletteColors = @{}
        for ($y = 0; $y -lt $reference.Height; $y++) {
            for ($x = 0; $x -lt $reference.Width; $x++) {
                $pixel = $reference.GetPixel($x, $y)
                if ($pixel.A -gt 0 -and $pixel.B -gt $pixel.R -and $pixel.B -gt $pixel.G) {
                    $paletteColors[$pixel.ToArgb()] = $pixel
                }
            }
        }
        $palette = @($paletteColors.Values | Sort-Object { 0.2126 * $_.R + 0.7152 * $_.G + 0.0722 * $_.B })
        if ($palette.Count -ne 5) { throw 'Unexpected native Fruits Delight blueberry palette' }
    } finally { $reference.Dispose(); $stream.Dispose() }
} finally { $berryArchive.Dispose() }
$audit = @()
foreach ($source in $sources) {
    $archive = [IO.Compression.ZipFile]::OpenRead($source.Path)
    try {
        foreach ($stage in 0..3) {
            $nativeName = $source.Prefix + "sweet_berry_bush_stage$stage"
            $name = $source.Prefix + "blue_berry_bush_stage$stage"
            $stream = $archive.GetEntry("assets/$($source.Namespace)/textures/block/$nativeName.png").Open()
            $original = [Drawing.Bitmap]::new($stream)
            $edited = [Drawing.Bitmap]$original.Clone()
            try {
                $changed = 0
                for ($y = 0; $y -lt $original.Height; $y++) {
                    for ($x = 0; $x -lt $original.Width; $x++) {
                        $pixel = $original.GetPixel($x, $y)
                        if ($pixel.A -gt 0 -and $pixel.R -gt 1.5 * $pixel.G -and $pixel.R -gt 1.5 * $pixel.B) {
                            # Vanilla's four red shades span R69..223. Map their
                            # shading into the installed fruit's five blue shades.
                            $shade = [Math]::Max(0, [Math]::Min($palette.Count - 1,
                                [int][Math]::Round(($pixel.R - 69) / 154.0 * ($palette.Count - 1))))
                            $blue = $palette[$shade]
                            $edited.SetPixel($x, $y, [Drawing.Color]::FromArgb($pixel.A, $blue.R, $blue.G, $blue.B))
                            $changed++
                        }
                    }
                }
                if (($stage -lt 2 -and $changed -ne 0) -or ($stage -ge 2 -and $changed -eq 0)) {
                    throw "Unexpected berry mask in $nativeName ($changed pixels)"
                }
                $destination = Join-Path $assets "textures/block/$name.png"
                $edited.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
                $saved = [Drawing.Bitmap]::FromFile($destination)
                try {
                    $otherChanges = 0
                    $alphaChanges = 0
                    $foreignBerryColors = 0
                    for ($y = 0; $y -lt $original.Height; $y++) {
                        for ($x = 0; $x -lt $original.Width; $x++) {
                            $before = $original.GetPixel($x, $y)
                            $after = $saved.GetPixel($x, $y)
                            if ($before.A -ne $after.A) { $alphaChanges++ }
                            $berry = $before.A -gt 0 -and $before.R -gt 1.5 * $before.G -and $before.R -gt 1.5 * $before.B
                            if (-not $berry -and $before.ToArgb() -ne $after.ToArgb()) { $otherChanges++ }
                            if ($berry -and -not $paletteColors.ContainsKey($after.ToArgb())) { $foreignBerryColors++ }
                        }
                    }
                    if ($otherChanges -ne 0 -or $alphaChanges -ne 0 -or $foreignBerryColors -ne 0) { throw "Palette or unchanged-pixel invariant failed in $name" }
                    $audit += [ordered]@{ texture = $name; berryPixels = $changed; otherPixelsChanged = $otherChanges; alphaPixelsChanged = $alphaChanges; foreignBerryColors = $foreignBerryColors; palette = 'fruitsdelight:blueberry' }
                } finally { $saved.Dispose() }
                $model = @{parent = 'minecraft:block/cross'; textures = @{cross = "aoemod:block/$name"}} | ConvertTo-Json -Depth 4
                [IO.File]::WriteAllText((Join-Path $assets "models/block/$name.json"), $model + "`n")
                if ($source.Prefix -eq '') {
                    $metadata = @{srm = @{model = "aoemod:block/snowy_blue_berry_bush_stage$stage"}} | ConvertTo-Json -Depth 4
                    [IO.File]::WriteAllText((Join-Path $assets "models/block/$name.json.mcmeta"), $metadata + "`n")
                }
            } finally { $edited.Dispose(); $original.Dispose(); $stream.Dispose() }
        }
    } finally { $archive.Dispose() }
}
$audit | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $evidence 'pixel-audit.json') -Encoding utf8
$audit | ForEach-Object { [PSCustomObject]$_ } | Format-Table
