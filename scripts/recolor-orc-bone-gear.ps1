$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$textureRoot = Join-Path $projectRoot 'src/main/resources/assets/aoemod/textures/entity/orcz'
$evidenceRoot = Join-Path $projectRoot 'tmp/orc-bone-gear-assets'
New-Item -ItemType Directory -Path $textureRoot, $evidenceRoot -Force | Out-Null
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'libs/pack/orcz-0.87-1.20.1.jar'))
$audit = @()
try {
    foreach ($name in @('orc_savage.png', 'orc_savage_shield.png')) {
        $stream = $archive.GetEntry("assets/orcz/textures/entities/$name").Open()
        $original = [Drawing.Bitmap]::new($stream)
        $edited = [Drawing.Bitmap]$original.Clone()
        try {
            $changed = 0
            for ($y = 0; $y -lt $original.Height; $y++) {
                for ($x = 0; $x -lt $original.Width; $x++) {
                    $pixel = $original.GetPixel($x, $y)
                    # Native tool UV island, plus the shield-only UV island.
                    # Warm wood/leather, green skin and transparent pixels stay
                    # untouched even where they share the same atlas rectangle.
                    $gear = ($x -ge 88 -and $x -lt 132 -and $y -ge 45 -and $y -lt 75) -or
                        ($name -eq 'orc_savage_shield.png' -and (($x -ge 132 -and $x -lt 205 -and $y -ge 86 -and $y -lt 128) -or
                            ($x -ge 98 -and $x -lt 132 -and $y -ge 124 -and $y -lt 126)))
                    $iron = $pixel.A -gt 0 -and $pixel.B -ge $pixel.R -and $pixel.G -le $pixel.B + 10
                    if ($gear -and $iron) {
                        $luminance = 0.2126 * $pixel.R + 0.7152 * $pixel.G + 0.0722 * $pixel.B
                        $light = [Math]::Max(0.0, [Math]::Min(1.0, ($luminance - 32.0) / 64.0))
                        $bone = [Drawing.Color]::FromArgb($pixel.A,
                            [int][Math]::Round(176 + 71 * $light),
                            [int][Math]::Round(161 + 80 * $light),
                            [int][Math]::Round(124 + 92 * $light))
                        $edited.SetPixel($x, $y, $bone)
                        $changed++
                    }
                }
            }
            if ($changed -eq 0) { throw "No iron pixels matched in $name" }
            $destination = Join-Path $textureRoot $name
            $edited.Save($destination, [Drawing.Imaging.ImageFormat]::Png)
            $saved = [Drawing.Bitmap]::FromFile($destination)
            try {
                $outsideChanges = 0
                $alphaChanges = 0
                for ($y = 0; $y -lt $original.Height; $y++) {
                    for ($x = 0; $x -lt $original.Width; $x++) {
                        $before = $original.GetPixel($x, $y)
                        $after = $saved.GetPixel($x, $y)
                        if ($before.A -ne $after.A) { $alphaChanges++ }
                        $gear = ($x -ge 88 -and $x -lt 132 -and $y -ge 45 -and $y -lt 75) -or
                            ($name -eq 'orc_savage_shield.png' -and (($x -ge 132 -and $x -lt 205 -and $y -ge 86 -and $y -lt 128) -or
                                ($x -ge 98 -and $x -lt 132 -and $y -ge 124 -and $y -lt 126)))
                        $iron = $before.A -gt 0 -and $before.B -ge $before.R -and $before.G -le $before.B + 10
                        if (-not ($gear -and $iron) -and $before.ToArgb() -ne $after.ToArgb()) { $outsideChanges++ }
                    }
                }
                if ($outsideChanges -ne 0 -or $alphaChanges -ne 0) { throw "Texture invariants changed in $name" }
                $audit += [ordered]@{texture=$name; width=$saved.Width; height=$saved.Height;
                    recoloredIronPixels=$changed; otherPixelsChanged=$outsideChanges; alphaPixelsChanged=$alphaChanges}
            } finally { $saved.Dispose() }
        } finally { $edited.Dispose(); $original.Dispose(); $stream.Dispose() }
    }
} finally { $archive.Dispose() }
$audit | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $evidenceRoot 'pixel-audit.json') -Encoding utf8
$audit | Format-Table
