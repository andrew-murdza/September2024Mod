$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$projectRoot = Split-Path -Parent $PSScriptRoot
$reviewRoot = Join-Path $projectRoot 'release/texture-review-2026-11-08'
$names = @('aoemod_arthropleura_view_2', 'hybrid_aquatic_lightfoot_crab_view_2', 'orcz_bigorcboss_view_3_above')
$sheet = [Drawing.Bitmap]::new(1440, 1500)
$graphics = [Drawing.Graphics]::FromImage($sheet)
try {
    $graphics.Clear([Drawing.Color]::FromArgb(24,35,43))
    $graphics.InterpolationMode = [Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    for ($row = 0; $row -lt $names.Count; $row++) {
        for ($column = 0; $column -lt 2; $column++) {
            $phase = @('before', 'after')[$column]
            $imagePath = Join-Path $reviewRoot ("screenshots/$phase/" + $names[$row] + '.png')
            $image = [Drawing.Bitmap]::FromFile($imagePath)
            try { $graphics.DrawImage($image, [Drawing.Rectangle]::new($column*720, $row*500, 720, 500)) }
            finally { $image.Dispose() }
        }
    }
    $sheet.Save((Join-Path $reviewRoot 'overview.png'), [Drawing.Imaging.ImageFormat]::Png)
} finally { $graphics.Dispose(); $sheet.Dispose() }
