# Direct palette edits requested by the user: no image generation or resampling.
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = Split-Path -Parent $PSScriptRoot
$inputRoot = Join-Path $PSScriptRoot '../tmp/nov8-worldgen-followup/plant-inputs/assets'
$outputRoot = Join-Path $PSScriptRoot '../src/main/resources/assets'
$minecraftJar = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'build/fg_cache/mcp') -Filter client.jar -Recurse |
    Where-Object { $_.Directory.Name -eq 'downloadClient' }) | Select-Object -First 1
if (!$minecraftJar) { throw 'Run the normal Gradle setup first to obtain the original Minecraft client assets.' }
foreach ($archivePath in @((Join-Path $projectRoot 'libs/pack/hearthandharvest-1.4.1.jar'), $minecraftJar.FullName)) {
    $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
    try {
        foreach ($entry in $archive.Entries) {
            if ($entry.FullName -notmatch '^assets/(hearthandharvest/textures/block/(cotton_stage\d|wild_cotton)|minecraft/textures/block/pitcher_crop_(bottom|top)_stage_\d)\.png$') { continue }
            $target = Join-Path (Split-Path -Parent $inputRoot) $entry.FullName
            [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
            [IO.Compression.ZipFileExtensions]::ExtractToFile($entry, $target, $true)
        }
    } finally { $archive.Dispose() }
}
$palettes = @{
 cotton = @{'303B21'='29491B'; '444D39'='336A22'; '4A5E3A'='448729'; '617743'='5BA034'}
 pitcher = @{'3C4A39'='29491B'; '476946'='2B561E'; '6C804F'='336A22'; '8B994D'='448729'; 'A5AD5E'='5BA034'; 'C2C06E'='7ABA45'}
}
$report=@()
foreach($kind in @('cotton','pitcher')) {
 $namespace=if($kind -eq 'cotton'){'hearthandharvest'}else{'minecraft'}
 $folder=Join-Path $inputRoot "$namespace/textures/block"
 $files=Get-ChildItem -LiteralPath $folder -Filter '*.png' | Where-Object { if($kind -eq 'cotton'){$_.Name -match '^(cotton_stage\d|wild_cotton)\.png$'}else{$_.Name -match '^pitcher_crop_(bottom|top)_stage_\d\.png$'} }
 foreach($file in $files) {
  $original=[System.Drawing.Bitmap]::new($file.FullName)
  $edited=[System.Drawing.Bitmap]::new($original.Width,$original.Height,[System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  $changed=0
  for($y=0;$y -lt $original.Height;$y++){for($x=0;$x -lt $original.Width;$x++){
   $color=$original.GetPixel($x,$y);$hex='{0:X2}{1:X2}{2:X2}' -f $color.R,$color.G,$color.B
   if($color.A -ne 0 -and $palettes[$kind].ContainsKey($hex)){
    $target=[System.Drawing.ColorTranslator]::FromHtml('#'+$palettes[$kind][$hex])
    $color=[System.Drawing.Color]::FromArgb($color.A,$target.R,$target.G,$target.B);$changed++
   }
   $edited.SetPixel($x,$y,$color)
  }}
  if($changed -gt 0) {
   $dest=Join-Path $outputRoot "$namespace/textures/block/$($file.Name)"
   [System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($dest)) | Out-Null
   $edited.Save($dest,[System.Drawing.Imaging.ImageFormat]::Png)
   $check=[System.Drawing.Bitmap]::new($dest)
   for($y=0;$y -lt $original.Height;$y++){for($x=0;$x -lt $original.Width;$x++){
    $before=$original.GetPixel($x,$y);$after=$check.GetPixel($x,$y)
    if($before.A -ne $after.A){throw "Alpha changed: $dest"}
    $hex='{0:X2}{1:X2}{2:X2}' -f $before.R,$before.G,$before.B
    if(-not $palettes[$kind].ContainsKey($hex) -and $before.ToArgb() -ne $after.ToArgb()){throw "Non-green pixel changed: $dest"}
   }}
   $check.Dispose()
   $report+=@{texture="$namespace/textures/block/$($file.Name)";width=$original.Width;height=$original.Height;changedGreenPixels=$changed;alphaAndOtherColorsUnchanged=$true;sha256=(Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash.ToLowerInvariant()}
  }
  $original.Dispose();$edited.Dispose()
 }
}
$report | ConvertTo-Json -Depth 5 | Set-Content (Join-Path $PSScriptRoot '../tmp/nov8-worldgen-followup/plant-recolor-report.json')
Write-Output "Recolored $($report.Count) textures; preserved dimensions, alpha, and every non-green pixel."
