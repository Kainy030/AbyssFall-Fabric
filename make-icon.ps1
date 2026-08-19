Add-Type -AssemblyName System.Drawing

$size = 128
$bmp  = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g    = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

# --- Background: dark radial gradient (the story's ominous tone) ---
$bgPath = New-Object System.Drawing.Drawing2D.GraphicsPath
$bgPath.AddEllipse(-30, -30, $size + 60, $size + 60)
$bgBrush = New-Object System.Drawing.Drawing2D.PathGradientBrush($bgPath)
$bgBrush.CenterColor    = [System.Drawing.Color]::FromArgb(255, 46, 30, 46)
$bgBrush.SurroundColors = @([System.Drawing.Color]::FromArgb(255, 16, 12, 20))
$g.FillRectangle($bgBrush, 0, 0, $size, $size)

# --- Peach blossom: 5 petals ("TaoYuan" = peach garden) ---
$cx = 64.0
$cy = 60.0
for ($i = 0; $i -lt 5; $i++) {
    $angle = $i * 72.0 - 90.0

    # Petal drawn pointing up from the origin: narrow where it meets the centre,
    # widening out and rounding off at the tip. Bezier control points rather than
    # a plain ellipse, so the silhouette reads as a blossom.
    $petal = New-Object System.Drawing.Drawing2D.GraphicsPath
    $petal.AddBezier(
        [single]0,   [single]0,       # base, at the flower centre
        [single]22,  [single](-17),
        [single]20,  [single](-43),
        [single]0,   [single](-46))   # rounded tip
    $petal.AddBezier(
        [single]0,   [single](-46),
        [single](-20), [single](-43),
        [single](-22), [single](-17),
        [single]0,   [single]0)
    $petal.CloseFigure()

    $m = New-Object System.Drawing.Drawing2D.Matrix
    $m.Translate($cx, $cy)
    $m.Rotate($angle + 90)
    $petal.Transform($m)

    $pr = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        (New-Object System.Drawing.Point([int]($cx - 40), [int]($cy - 46))),
        (New-Object System.Drawing.Point([int]($cx + 40), [int]($cy + 28))),
        [System.Drawing.Color]::FromArgb(255, 255, 214, 228),
        [System.Drawing.Color]::FromArgb(255, 232, 122, 160))
    $g.FillPath($pr, $petal)

    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(190, 150, 52, 84), 1.6)
    $g.DrawPath($pen, $petal)

    $pen.Dispose(); $pr.Dispose(); $petal.Dispose(); $m.Dispose()
}

# --- Flower centre ---
$cBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    (New-Object System.Drawing.Point([int]($cx - 11), [int]($cy - 11))),
    (New-Object System.Drawing.Point([int]($cx + 11), [int]($cy + 11))),
    [System.Drawing.Color]::FromArgb(255, 255, 226, 150),
    [System.Drawing.Color]::FromArgb(255, 226, 152, 60))
$g.FillEllipse($cBrush, $cx - 11, $cy - 11, 22, 22)
$cBrush.Dispose()

# --- Stamens ---
$sPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(220, 168, 92, 24), 1.5)
for ($i = 0; $i -lt 8; $i++) {
    $r = ($i * 45.0) * [Math]::PI / 180.0
    $g.DrawLine($sPen,
        [single]($cx + [Math]::Cos($r) * 3),  [single]($cy + [Math]::Sin($r) * 3),
        [single]($cx + [Math]::Cos($r) * 10), [single]($cy + [Math]::Sin($r) * 10))
}
$sPen.Dispose()

# --- Ten drifting petals: "Ten Days Till End" ---
# Laid out along a descending diagonal so they read as drifting on wind rather
# than piling up in a band. Petals further along the path are smaller and more
# transparent to suggest depth.
$rng = New-Object System.Random(20261011)

# Hand-placed anchors (x, y, scale) sweeping from upper-left to lower-right,
# deliberately avoiding the blossom itself (centred at 64,60 with r~34).
$anchors = @(
    @(12, 14, 1.00), @(34, 26, 0.80), @(9,  46, 0.70),
    @(16, 92, 0.95), @(42, 112, 0.80), @(70, 120, 0.65),
    @(100, 106, 0.90), @(119, 74, 0.75), @(112, 34, 0.60),
    @(94, 14, 0.85)
)

foreach ($a in $anchors) {
    $px = [double]$a[0]
    $py = [double]$a[1]
    $sc = [double]$a[2]

    # Jitter so the diagonal never looks mechanical.
    $px += $rng.Next(-4, 5)
    $py += $rng.Next(-4, 5)

    $w = 11.0 * $sc
    $h = $w * 0.60
    $alpha = [int](70 + 120 * $sc)

    # Teardrop petal: rounded at one end, tapering to a point at the other.
    # Built from two mirrored bezier curves rather than an ellipse, which is what
    # made the earlier version look like pills.
    $fp = New-Object System.Drawing.Drawing2D.GraphicsPath
    $hw = $w / 2.0
    $fp.AddBezier(
        [single]0,      [single](-$h),          # tip
        [single]$hw,    [single](-$h * 0.45),
        [single]$hw,    [single]($h * 0.45),
        [single]0,      [single]$h)             # rounded base
    $fp.AddBezier(
        [single]0,      [single]$h,
        [single](-$hw), [single]($h * 0.45),
        [single](-$hw), [single](-$h * 0.45),
        [single]0,      [single](-$h))
    $fp.CloseFigure()

    $fm = New-Object System.Drawing.Drawing2D.Matrix
    $fm.Translate([single]$px, [single]$py)
    $fm.Rotate([single]$rng.Next(0, 360))
    $fp.Transform($fm)

    $fb = New-Object System.Drawing.SolidBrush(
        [System.Drawing.Color]::FromArgb($alpha, 246, 168, 196))
    $g.FillPath($fb, $fp)

    $fb.Dispose(); $fp.Dispose(); $fm.Dispose()
}

# --- Subtle border ---
$bPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(70, 255, 205, 222), 2)
$g.DrawRectangle($bPen, 1, 1, $size - 3, $size - 3)
$bPen.Dispose()

$g.Dispose()
$bgBrush.Dispose(); $bgPath.Dispose()

$out = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\icon.png'
New-Item -ItemType Directory -Force -Path (Split-Path $out) | Out-Null
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output "saved: $out"
