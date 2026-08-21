Add-Type -AssemblyName System.Drawing

# Mob effect icon for "精神饱满" (Spirited).
#
# Designed as the visual answer to the Mental Breakdown icon, so the pair reads as one scale
# running in two directions: same 18x18 footprint, same central mass, but cool cyan instead of
# sickly green, and rising rays where the breakdown has a fracture. A player carrying both
# should be able to tell them apart in the corner of their eye.
#
# Vanilla mob effect icons are 18x18 with a transparent background. As with the other icons in
# this project the artwork is drawn at 8x and box-filtered down, so the small silhouette stays
# readable instead of turning to mush.

$target = 18
$ss     = 8
$size   = $target * $ss

$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g   = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.Clear([System.Drawing.Color]::Transparent)

$cx = $size / 2.0
$cy = $size / 2.0

# --- Rays: the "spirited" read ---
# Drawn first so the core sits on top of them. Eight spokes, the cardinals longer than the
# diagonals, which keeps the star from turning into a blur once it is 18px across.
$rayR = $size / 2.0 - 2
foreach ($i in 0..7) {
    $angle = $i * [Math]::PI / 4.0
    $long  = if ($i % 2 -eq 0) { 1.0 } else { 0.72 }

    $x1 = $cx + [Math]::Cos($angle) * $rayR * 0.30
    $y1 = $cy + [Math]::Sin($angle) * $rayR * 0.30
    $x2 = $cx + [Math]::Cos($angle) * $rayR * $long
    $y2 = $cy + [Math]::Sin($angle) * $rayR * $long

    $pen = New-Object System.Drawing.Pen(
        [System.Drawing.Color]::FromArgb(255, 126, 214, 206), 13.0)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawLine($pen, [single]$x1, [single]$y1, [single]$x2, [single]$y2)
    $pen.Dispose()
}

# --- Halo: softens the join between rays and core ---
$haloR = $rayR * 0.50
$haloPen = New-Object System.Drawing.Pen(
    [System.Drawing.Color]::FromArgb(200, 96, 190, 182), 11.0)
$g.DrawEllipse($haloPen, [single]($cx - $haloR), [single]($cy - $haloR), [single]($haloR * 2), [single]($haloR * 2))
$haloPen.Dispose()

# --- Core: a clear mind, bright at the centre ---
$coreR = $rayR * 0.44
$coreBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point([int]($cx - $coreR), [int]($cy - $coreR))),
    (New-Object System.Drawing.Point([int]($cx + $coreR), [int]($cy + $coreR))),
    [System.Drawing.Color]::FromArgb(255, 238, 252, 250),
    [System.Drawing.Color]::FromArgb(255,  84, 172, 168))
$g.FillEllipse($coreBrush, [single]($cx - $coreR), [single]($cy - $coreR), [single]($coreR * 2), [single]($coreR * 2))
$coreBrush.Dispose()

# --- Highlight: keeps the core from reading as a flat disc ---
$glintR = $coreR * 0.34
$glintBrush = New-Object System.Drawing.SolidBrush(
    [System.Drawing.Color]::FromArgb(230, 255, 255, 255))
$g.FillEllipse($glintBrush,
    [single]($cx - $coreR * 0.42 - $glintR), [single]($cy - $coreR * 0.42 - $glintR),
    [single]($glintR * 2), [single]($glintR * 2))
$glintBrush.Dispose()

$g.Dispose()

# --- Downscale to 18x18 ---
$out = New-Object System.Drawing.Bitmap($target, $target, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$og  = [System.Drawing.Graphics]::FromImage($out)
$og.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$og.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$og.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$og.Clear([System.Drawing.Color]::Transparent)
$og.DrawImage($bmp, (New-Object System.Drawing.Rectangle(0, 0, $target, $target)))
$og.Dispose()
$bmp.Dispose()

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\mob_effect\san_spirited.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

Write-Output "saved: $path"
