# Draws a debug mask for the Final Death Omen.
#
# WHAT THIS IS FOR
# ----------------
# Not artwork. This exists to make the shader system's behaviour VISIBLE, so that one look at the item
# in-game answers questions no amount of reading can:
#
#   - is the thickness being drawn at all, and on every edge
#   - which channel drives which part
#   - does the sampled channel actually flicker while the continuous one holds steady
#   - do the front, back and side faces all receive the effect
#
# So channels are assigned by REGION, in a pattern chosen to be unmistakable. Anything subtle would
# defeat the point.
#
# WHY NOT BY BRIGHTNESS
# ---------------------
# The previous version of this script told hilt from blade by pixel brightness. That worked on the old
# texture, which split cleanly into a bright blade and a near-black hilt. The current texture does not:
# luminance runs 0..185 with a median of 16 and no second peak, so any threshold puts nearly everything
# on one side -- measured at 89 pixels against 14.
#
# Geometry is a reliable signal where brightness is not. The blade runs diagonally across the top
# right, the guard and grip sit lower left, and that comes off the silhouette rather than off how dark
# something was painted.
#
# THE REGIONS
# -----------
#   BLUE  (sampled, flickers)  -> the blade, rows 0..4
#   GREEN (continuous, steady) -> the guard and grip, rows 5..15
#
# Two large contiguous areas with a single boundary. If that boundary is visible in-game and one side
# flickers while the other holds, every stage of the pipeline is working.

Add-Type -AssemblyName System.Drawing

$itemPath = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\final_death_omen.png'
$maskPath = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\final_death_omen_mask.png'

$item = New-Object System.Drawing.Bitmap($itemPath)
$size = 16

if ($item.Width -ne $size -or $item.Height -ne $size) {
	throw "Expected a ${size}x${size} item texture, found $($item.Width)x$($item.Height)"
}

$GREEN = [System.Drawing.Color]::FromArgb(255, 0, 255, 0)
$BLUE = [System.Drawing.Color]::FromArgb(255, 0, 0, 255)
$CLEAR = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

# Rows 0..4 are the blade's tip and edge, running diagonally down-left; row 5 is where the guard
# starts. Read off the silhouette, not guessed.
$BLADE_LAST_ROW = 4

$blade = 0
$grip = 0

$result = New-Object System.Drawing.Bitmap($size, $size)

for ($y = 0; $y -lt $size; $y++) {
	for ($x = 0; $x -lt $size; $x++) {
		# Nothing where the item is not. The texture is fully binary now, so this is exact: no pixel is
		# partly present, and no side face grows anywhere invisible.
		if ($item.GetPixel($x, $y).A -eq 0) {
			$result.SetPixel($x, $y, $CLEAR)
			continue
		}

		# Every solid pixel gets a channel, outline included. An unlit outline pixel is a gap in the
		# thickness, since a side face reads the mask at the pixel it grew from.
		if ($y -le $BLADE_LAST_ROW) {
			$result.SetPixel($x, $y, $BLUE)
			$blade++
		} else {
			$result.SetPixel($x, $y, $GREEN)
			$grip++
		}
	}
}

# Confirm every pixel that will grow a side face carries a channel. Vanilla's own test: a solid pixel
# with at least one transparent orthogonal neighbour. Checked here rather than trusted, because an
# uncovered outline pixel is exactly the failure this mask is meant to rule out.
$outlineCovered = 0
$outlineTotal = 0

for ($y = 0; $y -lt $size; $y++) {
	for ($x = 0; $x -lt $size; $x++) {
		if ($item.GetPixel($x, $y).A -eq 0) {
			continue
		}

		$isOutline = $false

		foreach ($step in @(@(0, -1), @(0, 1), @(-1, 0), @(1, 0))) {
			$nx = $x + $step[0]
			$ny = $y + $step[1]

			if ($nx -lt 0 -or $ny -lt 0 -or $nx -ge $size -or $ny -ge $size -or $item.GetPixel($nx, $ny).A -eq 0) {
				$isOutline = $true
			}
		}

		if ($isOutline) {
			$outlineTotal++
			$pixel = $result.GetPixel($x, $y)

			if ($pixel.A -gt 0 -and ($pixel.G -gt 0 -or $pixel.B -gt 0)) {
				$outlineCovered++
			}
		}
	}
}

$item.Dispose()

$result.Save($maskPath, [System.Drawing.Imaging.ImageFormat]::Png)
$result.Dispose()

Write-Output "Blade, sampled (blue):              $blade"
Write-Output "Guard and grip, continuous (green): $grip"
Write-Output "Outline pixels carrying a channel:  $outlineCovered of $outlineTotal"
Write-Output "Wrote $maskPath"

