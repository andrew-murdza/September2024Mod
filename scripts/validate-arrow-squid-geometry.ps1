$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
$assetRoot = Join-Path $root 'src/main/resources/assets/aoemod'
$geometryPath = Join-Path $assetRoot 'geo/cephalopod/arrow_squid/arrow_squid.geo.json'
$texturePath = Join-Path $assetRoot 'textures/entity/cephalopod/arrow_squid/arrow_squid.png'
$archive = [IO.Compression.ZipFile]::OpenRead((Join-Path $root 'libs/pack/hybrid_aquatic-1.7.4.jar'))

function Assert-Valid($condition, [string]$message) {
    if (!$condition) { throw $message }
}

function Read-ArchiveText([string]$path) {
    $reader = [IO.StreamReader]::new($archive.GetEntry($path).Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

function Same-Json($left, $right) {
    return ($left | ConvertTo-Json -Depth 30 -Compress) -eq ($right | ConvertTo-Json -Depth 30 -Compress)
}

function In-MantleBox([int]$x, [int]$y) {
    return (($x -lt 26 -and $y -ge 21 -and $y -lt 32) -or
            ($x -lt 30 -and $y -ge 12 -and $y -lt 21) -or
            ($x -lt 12 -and $y -ge 32 -and $y -lt 36))
}

try {
    $native = (Read-ArchiveText 'assets/hybrid_aquatic/geo/cephalopod/arrow_squid/arrow_squid.geo.json' | ConvertFrom-Json).'minecraft:geometry'[0]
    $revised = (Get-Content -LiteralPath $geometryPath -Raw | ConvertFrom-Json).'minecraft:geometry'[0]
    $bones = @{}
    $nativeBones = @{}
    foreach ($bone in $revised.bones) { $bones[$bone.name] = $bone }
    foreach ($bone in $native.bones) { $nativeBones[$bone.name] = $bone }
    Assert-Valid ($bones.Count -eq $nativeBones.Count) 'Animation bone count changed.'
    foreach ($name in $nativeBones.Keys) {
        Assert-Valid ($bones.ContainsKey($name)) "Missing animation bone $name."
        Assert-Valid ($bones[$name].parent -eq $nativeBones[$name].parent) "Parent changed for $name."
    }
    foreach ($name in @('body', 'fin')) {
        Assert-Valid (Same-Json $bones[$name] $nativeBones[$name]) "Unrequested change to $name."
    }
    Assert-Valid (Same-Json $bones['head'].cubes[0] $nativeBones['head'].cubes[0]) 'Head shape changed.'
    foreach ($side in @('left', 'right')) {
        $proximal = $bones["${side}_feeding_tentacle"].cubes[0]
        $distalBone = $bones["${side}_feeding_tentacle_2"]
        $club = $distalBone.cubes[0]
        $distal = $distalBone.cubes[1]
        Assert-Valid ($proximal.size[2] -eq 5.7 -and $distal.size[2] -eq 5.7) "$side feeding shaft length."
        Assert-Valid ([Math]::Abs($proximal.origin[2] + $proximal.size[2] - $distalBone.pivot[2]) -lt 0.00001) "$side joint gap."
        Assert-Valid ($distal.origin[2] -eq $distalBone.pivot[2]) "$side distal shaft misses joint."
        Assert-Valid ([Math]::Abs($distal.origin[2] + $distal.size[2] - $club.origin[2]) -lt 0.00001) "$side club gap."
        Assert-Valid ($club.size[2] -eq 3) "$side club size changed."
        $total = $club.origin[2] + $club.size[2] - $proximal.origin[2]
        Assert-Valid ([Math]::Abs($total / 9 - 1.6) -lt 0.00001) "$side total extension is not 60%."
        foreach ($level in @('top', 'bottom')) {
            $arm = $bones["${side}_${level}_tentacle"].cubes[0]
            Assert-Valid ($arm.size[2] -eq 11.4) "$side $level plain arm length."
            Assert-Valid ([Math]::Abs($arm.origin[2] + $arm.size[2] - $club.origin[2]) -lt 0.00001) "$side $level arm does not reach club base."
        }
    }
    $uvFaces = 0
    foreach ($bone in $revised.bones) {
        foreach ($cube in $bone.cubes) {
            if ($cube.uv -is [Array]) { continue }
            foreach ($face in $cube.uv.PSObject.Properties) {
                $uv = $face.Value.uv
                $size = $face.Value.uv_size
                foreach ($axis in 0..1) {
                    Assert-Valid ($uv[$axis] -ge 0 -and $uv[$axis] -le 64 -and
                        $uv[$axis] + $size[$axis] -ge 0 -and $uv[$axis] + $size[$axis] -le 64) "Out-of-bounds UV on $($bone.name)."
                }
                $uvFaces++
            }
        }
    }
    foreach ($eye in $bones['head'].cubes[1..2]) {
        Assert-Valid (Same-Json $eye.size @(0.02, 1.5, 1.5)) 'Eye physical size must be 1.5 x 1.5 with separated opposing faces.'
        Assert-Valid ([Math]::Abs([Math]::Abs($eye.origin[0] + $eye.size[0] / 2) - 2.05) -lt 0.00001) 'Eye side offset changed.'
        Assert-Valid ($eye.origin[1] + $eye.size[1] / 2 -eq 1.5 -and
            $eye.origin[2] + $eye.size[2] / 2 -eq 5) 'Eye center changed while enlarging it.'
        foreach ($face in @('east', 'west')) {
            Assert-Valid (Same-Json $eye.uv.$face.uv @(48, 0)) 'Eye patch origin mismatch.'
            Assert-Valid (Same-Json $eye.uv.$face.uv_size @(4, 4)) 'Eye patch must be 4 x 4.'
        }
    }

    $nativeStream = $archive.GetEntry('assets/hybrid_aquatic/textures/entity/cephalopod/arrow_squid/arrow_squid.png').Open()
    $memory = [IO.MemoryStream]::new()
    $nativeStream.CopyTo($memory)
    $nativeStream.Dispose()
    $memory.Position = 0
    $originalTexture = [Drawing.Bitmap]::new($memory)
    $texture = [Drawing.Bitmap]::new($texturePath)
    try {
        Assert-Valid ($texture.Width -eq 64 -and $texture.Height -eq 64) 'Mob atlas must stay 64 x 64.'
        $changedMantle = 0
        $black = 0
        $white = 0
        for ($y = 0; $y -lt 64; $y++) {
            for ($x = 0; $x -lt 64; $x++) {
                $before = $originalTexture.GetPixel($x, $y)
                $after = $texture.GetPixel($x, $y)
                $eye = $x -ge 48 -and $x -lt 52 -and $y -lt 4
                $mantle = In-MantleBox $x $y
                if (!$eye) {
                    Assert-Valid ($before.A -eq $after.A) "UV transparency changed at $x,$y."
                    if (!$mantle) {
                        Assert-Valid ($before.ToArgb() -eq $after.ToArgb()) "Protected tentacle/fin texture changed at $x,$y."
                    } elseif ($before.ToArgb() -ne $after.ToArgb()) {
                        $changedMantle++
                        Assert-Valid ($after.R -gt $after.G -and $after.R -gt $after.B) "Mantle replacement is not red at $x,$y."
                    }
                } else {
                    Assert-Valid ($after.A -eq 255) 'Eye patch must be opaque.'
                    if ($after.R -lt 50 -and $after.G -lt 50 -and $after.B -lt 50) { $black++ }
                    if ($after.R -gt 220 -and $after.G -gt 220 -and $after.B -gt 220) { $white++ }
                    if ($x -eq 48 -or $x -eq 51 -or $y -eq 0 -or $y -eq 3) {
                        Assert-Valid ($after.R -gt $after.G -and $after.R -gt $after.B) 'Eye border must use regular red.'
                    }
                }
            }
        }
        Assert-Valid ($changedMantle -gt 0) 'Mantle palette was not changed.'
        Assert-Valid ($black -gt 0 -and $white -gt 0) 'Eye requires a white iris and black pupil.'
        [ordered]@{
            animation_bones_preserved = $bones.Count
            feeding_tentacle_length = 14.4
            native_feeding_tentacle_length = 9
            plain_arm_length = 11.4
            club_length_preserved = 3
            explicit_uv_faces_checked = $uvFaces
            eye_model_size = 1.5
            eye_face_separation = 0.02
            eye_atlas_size = 4
            changed_mantle_texels = $changedMantle
            all_other_atlas_texels_preserved = $true
        } | ConvertTo-Json
    } finally {
        $texture.Dispose()
        $originalTexture.Dispose()
        $memory.Dispose()
    }
} finally {
    $archive.Dispose()
}
