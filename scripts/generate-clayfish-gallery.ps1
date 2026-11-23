[CmdletBinding()]
param(
    [string]$ClientJar = (Join-Path $env:USERPROFILE '.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'),
    [string]$Output = (Join-Path $PSScriptRoot '../release/clayfish-gallery.html')
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$archive = [IO.Compression.ZipFile]::OpenRead($ClientJar)
try {
    $html = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'clayfish-gallery.template.html'))
    foreach ($texture in @('tropical_b', 'tropical_b_pattern_6')) {
        $entry = $archive.GetEntry("assets/minecraft/textures/entity/fish/$texture.png")
        if ($null -eq $entry) { throw "Missing Minecraft texture: $texture" }
        $stream = $entry.Open()
        $bytes = [IO.MemoryStream]::new()
        try {
            $stream.CopyTo($bytes)
            $bytes.Position = 0
            $bitmap = [Drawing.Bitmap]::new($bytes)
            try {
                if ($bitmap.Width -ne 32 -or $bitmap.Height -ne 32) { throw "Unexpected texture dimensions: $texture" }
                $pixels = [byte[]]::new(32 * 32 * 4)
                for ($y = 0; $y -lt 32; $y++) {
                    for ($x = 0; $x -lt 32; $x++) {
                        $color = $bitmap.GetPixel($x, $y)
                        $index = ($y * 32 + $x) * 4
                        $pixels[$index] = $color.R
                        $pixels[$index + 1] = $color.G
                        $pixels[$index + 2] = $color.B
                        $pixels[$index + 3] = $color.A
                    }
                }
                $encoded = [Convert]::ToBase64String($pixels)
            } finally { $bitmap.Dispose() }
        }
        finally { $stream.Dispose(); $bytes.Dispose() }
        $html = $html.Replace("__$($texture.ToUpperInvariant())_RGBA__", $encoded)
    }
    $destination = [IO.Path]::GetFullPath($Output)
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
    [IO.File]::WriteAllText($destination, $html, [Text.UTF8Encoding]::new($false))
    Write-Output "Created self-contained gallery: $destination"
} finally { $archive.Dispose() }
