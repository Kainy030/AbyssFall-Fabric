Add-Type -AssemblyName System.Drawing

# Item texture for "终焉之花" (Flower of the End).
#
# This is the icon.png artwork scaled to a 16x16 item texture. Rather than
# downscaling the 128px PNG (which turns 1px petal edges to mush), the same vector
# geometry is redrawn at 8x and then box-filtered down, so edges land on pixel
# boundaries cleanly.

$target = 16
$ss     = 8                    # supersampling factor
$size   = $target * $ss        # 128, same coordinate space as make-icon.ps1

$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g   = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# --- Transparent background ---
# Item textures must be transparent, unlike the mod icon which has a dark plate.
$g.Clear([System.Drawing.Color]::Transparent)

# --- Peach blossom: 5 petals (same geometry as make-icon.ps1) ---
$cx = 64.0
$cy = 64.0
for ($i = 0; $i -lt 5; $i++) {
    $angle = $i * 72.0 - 90.0

    $petal = New-Object System.Drawing.Drawing2D.GraphicsPath
    $petal.AddBezier(
        [single]0,     [single]0,
        [single]26,    [single](-20),
        [single]24,    [single](-51),
        [single]0,     [single](-55))
    $petal.AddBezier(
        [single]0,     [single](-55),
        [single](-24), [single](-51),
        [single](-26), [single](-20),
        [single]0,     [single]0)
    $petal.CloseFigure()

    $m = New-Object System.Drawing.Drawing2D.Matrix
    $m.Translate($cx, $cy)
    $m.Rotate($angle + 90)
    $petal.Transform($m)

    $pr = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point([int]($cx - 48), [int]($cy - 55))),
        (New-Object System.Drawing.Point([int]($cx + 48), [int]($cy + 34))),
        [System.Drawing.Color]::FromArgb(255, 255, 214, 228),
        [System.Drawing.Color]::FromArgb(255, 232, 122, 160))
    $g.FillPath($pr, $petal)

    # Outline keeps the petals readable once scaled down to 16x16.
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(230, 150, 52, 84), 4.0)
    $g.DrawPath($pen, $petal)

    $pen.Dispose(); $pr.Dispose(); $petal.Dispose(); $m.Dispose()
}

# --- Flower centre ---
$cBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point([int]($cx - 14), [int]($cy - 14))),
    (New-Object System.Drawing.Point([int]($cx + 14), [int]($cy + 14))),
    [System.Drawing.Color]::FromArgb(255, 255, 226, 150),
    [System.Drawing.Color]::FromArgb(255, 226, 152, 60))
$g.FillEllipse($cBrush, $cx - 14, $cy - 14, 28, 28)
$cBrush.Dispose()

$cPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(230, 168, 92, 24), 3.5)
$g.DrawEllipse($cPen, $cx - 14, $cy - 14, 28, 28)
$cPen.Dispose()

$g.Dispose()

# --- Downscale to 16x16 with high-quality filtering ---
$out = New-Object System.Drawing.Bitmap($target, $target, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$og  = [System.Drawing.Graphics]::FromImage($out)
$og.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$og.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
$og.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
$og.Clear([System.Drawing.Color]::Transparent)
$og.DrawImage($bmp, (New-Object System.Drawing.Rectangle(0, 0, $target, $target)))
$og.Dispose()
$bmp.Dispose()

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\abyss_flower.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

Write-Output "saved: $path"
