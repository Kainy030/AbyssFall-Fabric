Add-Type -AssemblyName System.Drawing

# Mob effect icon for "精神崩溃" (Mental Breakdown).
#
# A deliberate placeholder. The brief was "use the vanilla poison icon for now", but shipping
# Mojang's own poison.png inside a GPL-3.0 mod would be redistributing their artwork, so this
# draws a poison-flavoured stand-in of our own instead: the same sickly green read at a glance,
# with a fracture through it for the "breakdown" half of the name.
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

# --- Skull dome: the poison read ---
# Sickly yellow-green, the colour vanilla's poison icon is recognised by.
$domeR = ($size / 2.0 - 8)
$domeBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point([int]($cx - $domeR), [int]($cy - $domeR))),
    (New-Object System.Drawing.Point([int]($cx + $domeR), [int]($cy + $domeR))),
    [System.Drawing.Color]::FromArgb(255, 148, 176,  92),
    [System.Drawing.Color]::FromArgb(255,  74,  98,  56))
$g.FillEllipse($domeBrush, [single]($cx - $domeR), [single]($cy - $domeR * 0.95), [single]($domeR * 2), [single]($domeR * 1.75))
$domeBrush.Dispose()

# --- Jaw ---
$jawBrush = New-Object System.Drawing.SolidBrush(
    [System.Drawing.Color]::FromArgb(255, 120, 146, 78))
$g.FillRectangle($jawBrush, [single]($cx - $domeR * 0.52), [single]($cy + $domeR * 0.62), [single]($domeR * 1.04), [single]($domeR * 0.48))
$jawBrush.Dispose()

# --- Sockets: two voids, which is what makes it read as a skull at 18px ---
$socketBrush = New-Object System.Drawing.SolidBrush(
    [System.Drawing.Color]::FromArgb(255, 22, 28, 20))
$socketR = $domeR * 0.30
foreach ($sx in @(-0.42, 0.42)) {
    $ox = $cx + $domeR * $sx
    $oy = $cy - $domeR * 0.16
    $g.FillEllipse($socketBrush, [single]($ox - $socketR), [single]($oy - $socketR), [single]($socketR * 2), [single]($socketR * 2))
}
$socketBrush.Dispose()

# --- Fracture: the "breakdown" half of the name ---
# A single hairline crack, drawn light so it reads as a break in the surface rather than as a
# third feature competing with the sockets.
$crackPen = New-Object System.Drawing.Pen(
    [System.Drawing.Color]::FromArgb(220, 232, 236, 222), 5.0)
$crack = @(
    (New-Object System.Drawing.PointF([single]($cx - $domeR * 0.06), [single]($cy - $domeR * 0.92))),
    (New-Object System.Drawing.PointF([single]($cx + $domeR * 0.18), [single]($cy - $domeR * 0.52))),
    (New-Object System.Drawing.PointF([single]($cx - $domeR * 0.10), [single]($cy - $domeR * 0.28))),
    (New-Object System.Drawing.PointF([single]($cx + $domeR * 0.10), [single]($cy + $domeR * 0.04)))
)
$g.DrawLines($crackPen, $crack)
$crackPen.Dispose()

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

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\mob_effect\san_breakdown.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

Write-Output "saved: $path"
