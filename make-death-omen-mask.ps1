# Generates the mask for the Final Death Omen by flood-filling the item's own line-art.
#
# WHAT THIS WRITES
# ----------------
# Inside the item's outline:  pure opaque red (255, 0, 0, 255).
# The outline itself:         fully transparent (0, 0, 0, 0).
# Outside the outline:        fully transparent (0, 0, 0, 0).
#
# The item texture is READ ONLY. It is never modified by this script.
#
# WHY FLOOD-FILL FROM THE EDGES
# -----------------------------
# The item texture is pure line art: every solid pixel is part of the black outline, everything else is
# transparent. The outline divides the 16x16 square into an inside and an outside. To colour the inside red
# we flood-fill the outside from the four edges (4-connected), treating outline pixels as walls; every
# transparent pixel that the fill did NOT reach is the interior.
#
# WHY FLAT RED RATHER THAN A GRADIENT
# -----------------------------------
# The starfield reads the mask's RED channel as its opacity, so flat 255 means "the sky shows through the
# whole interior at full strength, everywhere, equally". That makes the mask contribute nothing of its own
# to how the effect looks: whatever appears on screen is the field's own arithmetic and the item's interior
# shape, with no third variable in between.
#
# WHY THE OUTLINE IS TRANSPARENT
# ------------------------------
# The outline was the darkest pixels of the original art. If the mask kept them opaque, the starfield would
# draw stars *on* the lines, which would read as a different shape from the original art. Making them
# transparent leaves them to whatever colour the original item draws there, so the lines stay as lines and
# the interior glows with stars.
#
# 🔴 THIS MASK DRIVES starfield ONLY
# ----------------------------------
# masked_pulse reads GREEN (continuous) and BLUE (sampled) and ignores red. Both are zero here, so this file
# leaves that effect fully transparent -- intact, but with nothing to show, which on screen is
# indistinguishable from a broken effect. A previous version of this script wrote all three channels for
# exactly that reason. It was replaced deliberately, to isolate the starfield while it is being brought up.
# Restoring masked_pulse means writing green and blue again.
#
# ✅ IT IS ANIMATED NOW — 9 frames, as the reference's is
# ------------------------------------------------------
# This once said an animated mask was impossible in 26.2. That was wrong, and the reasoning is worth keeping
# because it was a good trap: only TextureAtlas implements TickableTexture, so a mask bound as its OWN texture
# does sit on frame zero forever and its .mcmeta is never read.
#
# The mistake was concluding "therefore a mask cannot animate". A mask does not have to be a standalone
# texture. Bound as an ATLAS SPRITE it ticks like any other sprite — which is exactly how the star sprites
# have been animating all along. Nothing had to be added either: vanilla's items.json already stitches
# `item/`, and same-named atlas definitions across packs are concatenated rather than overridden, so a mask
# under textures/item/ was in the atlas the entire time.
#
# The cost is one indirection: 0..1 now spans the whole sheet, so the shader maps through MASK_U0..MASK_V1
# defines that the renderer resolves at bake time.
#
# ⇒ This script may write a multi-frame strip. A .mcmeta beside it is read and honoured.

Add-Type -AssemblyName System.Drawing

$itemPath = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\final_death_omen.png'
$target   = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\final_death_omen_mask.png'
$metaPath = "$target.mcmeta"

if (-not (Test-Path $itemPath)) {
	throw "Not found: $itemPath"
}

$item = New-Object System.Drawing.Bitmap($itemPath)

if ($item.Width -ne 16 -or $item.Height -ne 16) {
	throw "Expected a 16x16 item texture, found $($item.Width)x$($item.Height)"
}

# Read the line art first and release the item file immediately, so the source can never be written to
# even by accident. Every solid pixel is part of the black outline.
$outline = New-Object 'bool[,]' 16, 16
$outlineCount = 0

for ($y = 0; $y -lt 16; $y++) {
	for ($x = 0; $x -lt 16; $x++) {
		if ($item.GetPixel($x, $y).A -gt 0) {
			$outline[$x, $y] = $true
			$outlineCount++
		}
	}
}

$item.Dispose()

if ($outlineCount -eq 0) {
	throw 'The item texture is entirely transparent; nothing to mask.'
}

# Flood-fill the OUTSIDE from all four edges, treating outline pixels as walls. What remains unfilled —
# and is not itself an outline pixel — is interior.
$outside = New-Object 'bool[,]' 16, 16
$queue = New-Object System.Collections.Queue

# Seed from every pixel on the border that isn't part of the outline.
for ($x = 0; $x -lt 16; $x++) {
	foreach ($y in 0, 15) {
		if (-not $outline[$x, $y] -and -not $outside[$x, $y]) {
			$outside[$x, $y] = $true
			$queue.Enqueue(@($x, $y))
		}
	}
}
for ($y = 0; $y -lt 16; $y++) {
	foreach ($x in 0, 15) {
		if (-not $outline[$x, $y] -and -not $outside[$x, $y]) {
			$outside[$x, $y] = $true
			$queue.Enqueue(@($x, $y))
		}
	}
}

while ($queue.Count -gt 0) {
	$p = $queue.Dequeue()
	$px = $p[0]
	$py = $p[1]

	foreach ($d in @(@(1, 0), @(-1, 0), @(0, 1), @(0, -1))) {
		$nx = $px + $d[0]
		$ny = $py + $d[1]

		if ($nx -ge 0 -and $nx -lt 16 -and $ny -ge 0 -and $ny -lt 16 -and
		    -not $outline[$nx, $ny] -and -not $outside[$nx, $ny]) {
			$outside[$nx, $ny] = $true
			$queue.Enqueue(@($nx, $ny))
		}
	}
}

# Interior = not outline AND not outside. Count them for the report.
$interiorCount = 0
for ($y = 0; $y -lt 16; $y++) {
	for ($x = 0; $x -lt 16; $x++) {
		if (-not $outline[$x, $y] -and -not $outside[$x, $y]) {
			$interiorCount++
		}
	}
}

if ($interiorCount -eq 0) {
	throw 'No interior pixels found — the line art is not closed.'
}

$RED         = [System.Drawing.Color]::FromArgb(255, 255, 0, 0)
$TRANSPARENT = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

$result = New-Object System.Drawing.Bitmap(16, 16)

for ($y = 0; $y -lt 16; $y++) {
	for ($x = 0; $x -lt 16; $x++) {
		$isInterior = -not $outline[$x, $y] -and -not $outside[$x, $y]
		if ($isInterior) {
			$result.SetPixel($x, $y, $RED)
		} else {
			# Outline pixels AND exterior pixels are both transparent.
			$result.SetPixel($x, $y, $TRANSPARENT)
		}
	}
}

$result.Save($target, [System.Drawing.Imaging.ImageFormat]::Png)
$result.Dispose()

# Any .mcmeta left over from an earlier animated version would be dead weight -- see the note at the top.
# ⚠️ A .mcmeta beside the mask is now MEANINGFUL — it drives the animation (see the note at the top). This
# script writes a single frame, so it does not write one; but it must not delete an existing one either,
# because that would silently turn an animated mask into a still one.
if (Test-Path $metaPath) {
	Write-Output "Left existing $metaPath alone (it drives the mask's animation)"
	Write-Output "  -> this script wrote ONE frame; if that .mcmeta names more, the mask will be wrong"
}

Write-Output "Outline (line art, transparent): $outlineCount px"
Write-Output "Red (starfield interior):       $interiorCount px"
Write-Output "Exterior (transparent):         $((16 * 16) - $outlineCount - $interiorCount) px"
Write-Output "Wrote $target"
Write-Output "Item texture left untouched: $itemPath"
