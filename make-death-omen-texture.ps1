# Flattens an item texture's alpha to fully opaque or fully transparent.
#
# WHY THIS IS NECESSARY
# ---------------------
# Minecraft decides an item's SHAPE from its texture's alpha, and it uses a strict test:
# SpriteContents.isTransparent is `ARGB.alpha(pixel) == 0`. Anything with alpha 1 or above counts as
# solid and takes part in building geometry.
#
# ItemModelGenerator then extrudes the sprite into a box 1/16 thick and walls the silhouette with one
# side face per outline pixel. Those side faces are the item's visible THICKNESS.
#
# A pixel with alpha 1 is 0.4% opaque -- invisible on screen, yet solid as far as the model is
# concerned. Side faces grow along those pixels instead of along the edge the player can see, so the
# thickness ends up drawn where nothing is visible. Measured on the previous texture: 76 of its 116
# side faces sat on pixels averaging alpha 6.6/255, which is why the blade appeared to have no
# thickness at all no matter what was drawn over it.
#
# Semi-transparent pixels in the 11..254 range are usually anti-aliasing from an image editor. They
# are not wrong in themselves, but they have the same effect on geometry, and vanilla item art does
# not use them: pixel art draws a hard edge, and REFERENCE.md section 11 already records that
# anti-aliasing ruins single-pixel strokes at this size.
#
# So every pixel is pushed to one extreme. The silhouette the player sees becomes exactly the
# silhouette the model is built from, and the thickness appears where it belongs.
#
# WHAT THIS COSTS
# ---------------
# Edges become hard pixel steps rather than smooth gradients. That is the vanilla look, not a
# regression -- but it is a visible change, and it is the reason this is a deliberate step rather
# than something done silently on import.

param(
	# Alpha at or above this becomes opaque; below becomes transparent. 128 splits the range evenly,
	# which keeps a pixel the artist drew as "mostly there" and drops one drawn as "barely there".
	[int] $Threshold = 128
)

Add-Type -AssemblyName System.Drawing

$source = Join-Path $PSScriptRoot 'sword_NEW.png'
$target = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\final_death_omen.png'

if (-not (Test-Path $source)) {
	throw "Not found: $source"
}

$image = New-Object System.Drawing.Bitmap($source)

if ($image.Width -ne 16 -or $image.Height -ne 16) {
	throw "Expected 16x16, found $($image.Width)x$($image.Height)"
}

$result = New-Object System.Drawing.Bitmap(16, 16)

$madeOpaque = 0
$madeClear = 0
$untouched = 0

for ($y = 0; $y -lt 16; $y++) {
	for ($x = 0; $x -lt 16; $x++) {
		$pixel = $image.GetPixel($x, $y)

		if ($pixel.A -eq 0 -or $pixel.A -eq 255) {
			$result.SetPixel($x, $y, $pixel)
			$untouched++
			continue
		}

		if ($pixel.A -ge $Threshold) {
			# Keep the colour, drop the transparency. The colour was authored for this pixel; only its
			# alpha was ambiguous.
			$result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $pixel.R, $pixel.G, $pixel.B))
			$madeOpaque++
		} else {
			# Fully transparent, and colourless with it: a stray colour behind zero alpha still shows up
			# in some tooling and in the particle sprite picker.
			$result.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
			$madeClear++
		}
	}
}

$image.Dispose()

$result.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
$result.Dispose()

Write-Output "Threshold:            $Threshold"
Write-Output "Already 0 or 255:     $untouched"
Write-Output "Pushed to opaque:     $madeOpaque"
Write-Output "Pushed to clear:      $madeClear"
Write-Output "Wrote $target"
