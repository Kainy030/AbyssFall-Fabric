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
# WHY IT IS NOT ANIMATED, THOUGH THE REFERENCE'S IS
# -------------------------------------------------
# The reference's mask is 16x144 -- nine frames -- and breathes. Ours cannot, yet: a mask is bound as a
# standalone texture by the render setup, and in 26.2 only TextureAtlas implements TickableTexture. A .mcmeta
# beside a standalone texture is simply never read, so an animated mask would sit permanently on frame 0.
#
# The stars themselves DO animate, because they live in the item atlas -- which is the animation that matters,
# and the one the effect's shimmer comes from.

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
if (Test-Path $metaPath) {
	Remove-Item $metaPath -Force
	Write-Output "Removed stale $metaPath"
}

Write-Output "Outline (line art, transparent): $outlineCount px"
Write-Output "Red (starfield interior):       $interiorCount px"
Write-Output "Exterior (transparent):         $((16 * 16) - $outlineCount - $interiorCount) px"
Write-Output "Wrote $target"
Write-Output "Item texture left untouched: $itemPath"
