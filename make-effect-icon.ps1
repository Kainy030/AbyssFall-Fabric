Add-Type -AssemblyName System.Drawing

# Mob effect icon for "深渊探索者" (Abyss Explorer).
#
# Vanilla mob effect icons are 18x18 with a transparent background. As with the item
# texture, the artwork is drawn at 8x and box-filtered down so the small silhouette stays
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

# --- Descending rings: an eye looking down into the abyss ---
# Three concentric rings fading inwards read as depth at icon scale, where a literal
# "hole" would just look like a blob.
$rings = @(
    @(0.92, 235, 155, 110, 200),
    @(0.66, 245, 110,  70, 165),
    @(0.42, 250,  70,  40, 120)
)

foreach ($r in $rings) {
    $scale = [double]$r[0]
    $alpha = [int]$r[1]
    $cr    = [int]$r[2]
    $cg    = [int]$r[3]
    $cb    = [int]$r[4]

    $rad = ($size / 2.0 - 6) * $scale
    $pen = New-Object System.Drawing.Pen(
        [System.Drawing.Color]::FromArgb($alpha, $cr, $cg, $cb), 9.0)
    $g.DrawEllipse($pen, [single]($cx - $rad), [single]($cy - $rad), [single]($rad * 2), [single]($rad * 2))
    $pen.Dispose()
}

# --- Core: the point of no return ---
$coreR = ($size / 2.0 - 6) * 0.22
$coreBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point([int]($cx - $coreR), [int]($cy - $coreR))),
    (New-Object System.Drawing.Point([int]($cx + $coreR), [int]($cy + $coreR))),
    [System.Drawing.Color]::FromArgb(255, 246, 168, 196),
    [System.Drawing.Color]::FromArgb(255, 150, 52, 84))
$g.FillEllipse($coreBrush, [single]($cx - $coreR), [single]($cy - $coreR), [single]($coreR * 2), [single]($coreR * 2))
$coreBrush.Dispose()

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

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\mob_effect\abyss_explorer.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

Write-Output "saved: $path"
