$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$projectRoot = Split-Path -Parent $PSScriptRoot
$destination = Join-Path $projectRoot 'src/main/resources/assets/aoemod/textures/entity/orcz/orc_savage_big_2sword.png'
$evidenceRoot = Join-Path $projectRoot 'tmp/orc-boss-swords'
New-Item -ItemType Directory -Path $evidenceRoot -Force | Out-Null
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $projectRoot 'libs/pack/orcz-0.87-1.20.1.jar'))
$stream = $archive.GetEntry('assets/orcz/textures/entities/orc_savage_big_2sword.png').Open()
$original = [Drawing.Bitmap]::new($stream)
$edited = [Drawing.Bitmap]$original.Clone()
try {
    # ModelOrc_big_savage_2sword uses a 128x128 UV sheet on a 256x256 atlas.
    # The shared (0,18) sword UV also overlaps two head details. Move that
    # island to unused (80,80), retaining every original pixel for the head.
    # BoneBossSwordUV redirects only those sword texOffs calls to the copy.
    $swordCubes = @(@(0,18,1,4,6), @(0,18,1,3,15), @(19,45,1,3,11))
    $mask = [Collections.Generic.HashSet[int]]::new()
    $copies = [Collections.Generic.Dictionary[int,int]]::new()
    foreach ($cube in $swordCubes) {
        $u,$v,$w,$h,$d = $cube
        foreach ($rect in @(@(($u+$d),$v,($u+$d+2*$w),($v+$d)), @($u,($v+$d),($u+2*$d+2*$w),($v+$d+$h)))) {
            for ($y = 2*$rect[1]; $y -lt 2*$rect[3]; $y++) {
                for ($x = 2*$rect[0]; $x -lt 2*$rect[2]; $x++) {
                    if ($u -eq 0 -and $v -eq 18) {
                        $target = ($y+124)*256+$x+160
                        $copies[$target] = $y*256+$x
                        [void]$mask.Add($target)
                    } else { [void]$mask.Add($y*256+$x) }
                }
            }
        }
    }
    $changed = 0
    for ($y = 0; $y -lt $original.Height; $y++) {
        for ($x = 0; $x -lt $original.Width; $x++) {
            $pixel = $original.GetPixel($x,$y)
            if ($copies.ContainsKey($y*256+$x)) {
                if ($pixel.A -ne 0) { throw 'Sword UV destination overlaps native content' }
                $source = $copies[$y*256+$x]
                $pixel = $original.GetPixel($source%256,[int][Math]::Floor($source/256))
                $edited.SetPixel($x,$y,$pixel)
            }
            if (-not $mask.Contains($y*256+$x) -or $pixel.A -eq 0 -or $pixel.B -lt $pixel.R -or $pixel.G -gt $pixel.B+10) { continue }
            $luminance = .2126*$pixel.R + .7152*$pixel.G + .0722*$pixel.B
            $light = [Math]::Max(0.0,[Math]::Min(1.0,($luminance-32.0)/64.0))
            $edited.SetPixel($x,$y,[Drawing.Color]::FromArgb($pixel.A,
                [int][Math]::Round(176+71*$light),[int][Math]::Round(161+80*$light),[int][Math]::Round(124+92*$light)))
            $changed++
        }
    }
    if ($changed -eq 0) { throw 'No sword metal pixels found' }
    $edited.Save($destination,[Drawing.Imaging.ImageFormat]::Png)
    $saved = [Drawing.Bitmap]::FromFile($destination)
    try {
        $outsideChanges = 0; $alphaChanges = 0; $copiedPixels = 0
        for ($y = 0; $y -lt $original.Height; $y++) {
            for ($x = 0; $x -lt $original.Width; $x++) {
                $before = $original.GetPixel($x,$y); $after = $saved.GetPixel($x,$y)
                if ($copies.ContainsKey($y*256+$x)) {
                    $source = $copies[$y*256+$x]
                    $before = $original.GetPixel($source%256,[int][Math]::Floor($source/256))
                    if ($before.A -gt 0) { $copiedPixels++ }
                }
                if ($before.A -ne $after.A) { $alphaChanges++ }
                $metal = $mask.Contains($y*256+$x) -and $before.A -gt 0 -and $before.B -ge $before.R -and $before.G -le $before.B+10
                if (-not $metal -and $before.ToArgb() -ne $after.ToArgb()) { $outsideChanges++ }
            }
        }
        if ($outsideChanges -ne 0 -or $alphaChanges -ne 0) { throw 'Sword recolor changed unrelated pixels or alpha' }
        $audit = [ordered]@{texture='orc_savage_big_2sword.png';width=$saved.Width;height=$saved.Height;
            recoloredIronPixels=$changed;copiedSwordUvPixels=$copiedPixels;
            otherPixelsChanged=$outsideChanges;alphaPixelsChanged=$alphaChanges}
        $audit | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $evidenceRoot 'pixel-audit.json') -Encoding utf8
        $audit
    } finally { $saved.Dispose() }
} finally { $edited.Dispose();$original.Dispose();$stream.Dispose();$archive.Dispose() }
