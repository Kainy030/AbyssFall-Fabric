Add-Type -AssemblyName System.Drawing

# ============================================================================
#  Item texture for the Cognition Lens (abyssfall:san_lens).
#
#  Run it with:
#      powershell -NoProfile -ExecutionPolicy Bypass -File .\make-lens-icon.ps1
#
#  It writes four things:
#    1. an animated 16x48 strip into src\main\resources\...\textures\item\san_lens.png
#    2. san_lens.png.mcmeta beside it, which is what makes the strip animate
#    3. a static 16x16 gold_lens.png, the same mirror with no eye in it
#    4. a magnified preview into build\lens-icon-preview.png, outside the repository
#
#  To change the artwork, edit the [PATTERN] and [PALETTE] blocks below and run
#  it again. Nothing after them needs touching.
#
#  Drawn pixel by pixel, like make-dev-icon.ps1 and make-san-icon.ps1 and for the
#  same reason: at 16x16 any antialiasing turns a 1px stroke into a grey smear.
#  Every pixel here is fully opaque or fully transparent.
#
#  The shape is a whole standing mirror with no handle, filling the sprite: a gilt
#  frame around a very dark glass with an eye in it. Ornate gold against near-black
#  is the register the artwork is after, and an eye is what makes the mirror look
#  back rather than merely reflect.
#
#  The frame is lit from the upper left, so the top and left of it are bright gold
#  and the bottom and right are tarnished, and the glass darkens towards the bottom
#  for the same reason.
#
#  The eye's geometry is not invented. It follows vanilla's own ender_eye.png, read
#  pixel by pixel out of the 26.2 client jar, because an eye at this size only reads
#  as an eye under fairly narrow conditions:
#
#    * the pupil is ONE pixel wide and three tall, not a 2x2 block. A square pupil
#      surrounded by iris reads as a doughnut at every scale that matters.
#    * a dark ring sits between pupil and iris ('d' here, 1E4835 in vanilla). This
#      middle value is what stops the two from merging into one shape.
#    * the iris carries a specular glint in its upper left ('s'). Vanilla's is
#      CBFCDD. It breaks the symmetry that otherwise makes the eye read as a letter.
#    * the iris hue must oppose the frame's. Gold iris inside a gold frame merges
#      into a coin, so the iris here is a cold blue-grey.
# ============================================================================


# ---------------------------------------------------------------------------
# [PATTERN] 16 rows x 16 columns. One character is one pixel.
#
#   .  = transparent
#   O  = black outline
#   A  = frame, lit gold
#   B  = frame, tarnished gold
#   1  = glass, upper
#   2  = glass, lower
#   3  = reflection streak on the glass
#   h  = the iris
#   s  = specular glint on the iris
#   k  = iris shade
#   d  = dark ring around the pupil
#   p  = the pupil
#
# Every symbol is a distinct character rather than a case pair, because PowerShell
# hash literal keys are case-insensitive: a palette keyed by 'A' and 'a' will not
# even parse.
#
# Column ->  0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15
# ---------------------------------------------------------------------------
$pattern = @(
    '.....OOOOOO.....',   # row 0
    '....OAAAAAAO....',   # row 1
    '...OA111111BO...',   # row 2
    '..OA13111111BO..',   # row 3
    '..OA33111111BO..',   # row 4
    '.OA111shhhh11BO.',   # row 5
    '.OA11shhdhhk1BO.',   # row 6
    '.OA1shhdpdhhkBO.',   # row 7
    '.OA1hhhdpdhhkBO.',   # row 8
    '.OA22hhdpdhk2BO.',   # row 9
    '.OB222hhdhk22BO.',   # row 10
    '..OB22111122BO..',   # row 11
    '..OB22222222BO..',   # row 12
    '...OB222222BO...',   # row 13
    '....OBBBBBBO....',   # row 14
    '.....OOOOOO.....'    # row 15
)


# ---------------------------------------------------------------------------
# [PALETTE] Six hex digits per symbol. Change the numbers, nothing else.
#
# No relation to the San purples used elsewhere in the mod: this item is not a
# readout of the value, and gold on near-black is what the requested register
# needs.
#
# The two golds are one hue at two values rather than two hues, so the frame reads
# as a single metal catching light unevenly. The glass is a very dark violet
# instead of a neutral black so that it still reads as a colour at inventory size,
# and the eye is a cold off-white rather than pure white, which would glare.
# ---------------------------------------------------------------------------
$colors = @{
    'O' = '000000'   # outline: black
    'A' = 'F2D089'   # frame, lit: bright gold
    'B' = '8C6A28'   # frame, shaded: tarnished gold
    '1' = '2A1F3D'   # glass, upper: dark violet
    '2' = '150E20'   # glass, lower: darker still
    '3' = '5E4E7A'   # reflection: greyed violet
    'h' = 'BFC8DE'   # the iris: cold pale blue-grey, opposed in hue to the frame
    's' = 'F4F7FF'   # specular glint, upper left of the iris, as the ender eye has
    'k' = '6E7690'   # iris shade, lower right
    'd' = '2E3348'   # the dark ring that wraps the pupil, as vanilla's 1E4835 does.
                     # Without this middle value the pupil merges into the iris and
                     # the eye reads as a ring rather than as an eye.
    'p' = '07040C'   # the pupil: near black
}


# ---------------------------------------------------------------------------
# [ANIMATION] The eye glances about.
#
# The pattern above is the eye looking straight ahead. Each frame is that pattern
# with the pupil and its dark ring shifted sideways, so the frame, the glass and the
# iris all stay put and only the gaze moves. Anything else would read as the whole
# mirror rotating rather than as something inside it looking around.
#
# There are only three distinct images -- looking left, ahead and right -- so those
# are the only three written into the strip. The sequence below then refers to them
# by index as often as it likes, which is why the file is 16x48 rather than one
# square per step of the animation.
#
# A shift of one pixel is the whole available travel: the iris spans columns 5 to 10
# and the pupil with its ring is three columns wide, so a shift of two would push
# the ring out onto the glass and the eye would come apart.
$gazeFrames = @(0, -1, 1)   # index 0 = ahead, 1 = left, 2 = right

# The sequence, as {frame, ticks}. Twenty ticks is one second.
#
# The timings are deliberately uneven, and that unevenness is the whole point. An
# even cadence reads as a machine sweeping a beam; what makes a gaze feel haughty is
# resting a long while looking straight through you and then only briefly deigning
# to glance aside. So the ahead frames are held for two to four seconds and the
# glances away for well under one, and no two intervals repeat.
$gazeSequence = @(
    @{ Frame = 0; Ticks = 70 },   # a long, level stare
    @{ Frame = 1; Ticks = 18 },   # a brief glance left
    @{ Frame = 0; Ticks = 45 },
    @{ Frame = 2; Ticks = 14 },   # an even briefer one right
    @{ Frame = 0; Ticks = 85 },   # the longest hold of the loop
    @{ Frame = 2; Ticks = 22 },
    @{ Frame = 0; Ticks = 38 },
    @{ Frame = 1; Ticks = 12 }    # the quickest flick of all
)


# ===========================  no edits needed below  ========================

$size = 16

function Convert-Hex([string]$hex) {
    $r = [Convert]::ToInt32($hex.Substring(0, 2), 16)
    $g = [Convert]::ToInt32($hex.Substring(2, 2), 16)
    $b = [Convert]::ToInt32($hex.Substring(4, 2), 16)
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

# The pattern is the specification, so a typo in it should stop the script rather
# than quietly produce a sprite with a stray column.
if ($pattern.Count -ne $size) {
    throw "The pattern has $($pattern.Count) rows; it needs exactly $size."
}

for ($y = 0; $y -lt $size; $y++) {
    if ($pattern[$y].Length -ne $size) {
        throw "Row $y is $($pattern[$y].Length) characters wide; it needs exactly $size."
    }
}

$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

# Which symbols travel with the gaze. Everything else is scenery and stays where
# the pattern put it.
$gazeSymbols = @('p', 'd')

# What sits underneath the pupil once it has moved off a column. The iris is the
# only correct answer: the gaze slides across the iris, so the vacated column has
# to become iris rather than glass.
$gazeBackdrop = 'h'

function New-Frame([int]$shift) {
    $frame = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    for ($y = 0; $y -lt $size; $y++) {
        $row = $pattern[$y]

        # Where the travelling symbols are on this row, before the shift.
        $moving = @{}

        for ($x = 0; $x -lt $size; $x++) {
            if ($gazeSymbols -contains [string]$row[$x]) {
                $moving[$x] = [string]$row[$x]
            }
        }

        for ($x = 0; $x -lt $size; $x++) {
            $symbol = [string]$row[$x]

            # A column the gaze has moved onto takes the travelling symbol from
            # wherever it came from; a column it has left falls back to iris.
            $source = $x - $shift

            if ($moving.ContainsKey($source)) {
                $symbol = $moving[$source]
            } elseif ($moving.ContainsKey($x)) {
                $symbol = $gazeBackdrop
            }

            if ($symbol -eq '.') {
                # Written out rather than left to the bitmap's zeroed memory, so
                # the file never depends on that.
                $frame.SetPixel($x, $y, $clear)
                continue
            }

            if (-not $colors.ContainsKey($symbol)) {
                throw "Row $y column $x uses '$symbol', which has no colour."
            }

            $frame.SetPixel($x, $y, (Convert-Hex $colors[$symbol]))
        }
    }

    return $frame
}

$frames = @()

foreach ($offset in $gazeFrames) {
    $frames += (New-Frame $offset)
}

# One tall strip, frames stacked top to bottom, which is how Minecraft reads an
# animated texture: the width is the frame size and each square down the strip is
# the next frame. Only the distinct poses are stored, not every step of the loop.
$strip = New-Object System.Drawing.Bitmap($size, ($size * $frames.Count), [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$stripGraphics = [System.Drawing.Graphics]::FromImage($strip)
$stripGraphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy

for ($i = 0; $i -lt $frames.Count; $i++) {
    $stripGraphics.DrawImage($frames[$i], (New-Object System.Drawing.Rectangle 0, ($i * $size), $size, $size))
}

$stripGraphics.Dispose()

foreach ($frame in $frames) {
    $frame.Dispose()
}

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\san_lens.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$strip.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$strip.Dispose()

Write-Output "saved: $path  ($($frames.Count) unique frames)"

# The companion metadata. Without this the strip would be read as one very tall
# sprite and drawn squashed into the slot.
#
# Written as explicit {index,time} entries rather than a uniform 'frametime' so
# each step of the loop can hold for a different length. The unevenness is what
# reads as a gaze rather than as a sweep.
$frameEntries = ($gazeSequence | ForEach-Object {
    '    { "index": ' + $_.Frame + ', "time": ' + $_.Ticks + ' }'
}) -join ",`n"

$meta = @"
{
  "animation": {
    "frames": [
$frameEntries
    ]
  }
}
"@

$metaPath = "$path.mcmeta"
[System.IO.File]::WriteAllText($metaPath, $meta, (New-Object System.Text.UTF8Encoding $false))

Write-Output "saved: $metaPath"


# ---------------------------------------------------------------------------
# The Gold Lens (abyssfall:gold_lens): the same mirror with nothing looking out of
# it. One static 16x16 frame, no mcmeta.
#
# Generated here rather than from its own script on purpose. The two items share a
# frame, a glass and a palette, so splitting them would mean editing the gold in two
# places and letting them drift apart. What differs is only whether the eye is drawn.
#
# The eye's symbols fall back to glass rather than to a flat fill, so the vacated
# area keeps the same top-to-bottom darkening the rest of the glass has. A single
# colour there would read as a hole in the mirror instead of as an empty mirror.
# ---------------------------------------------------------------------------

$eyeSymbols = @('h', 's', 'k', 'd', 'p')

# Which glass value replaces the eye, by row. Rows 5 to 7 are still in the upper
# half of the glass, rows 8 to 10 in the lower, matching how the pattern shades the
# glass either side of the eye.
function Get-GlassFor([int]$row) {
    if ($row -le 7) {
        return '1'
    }

    return '2'
}

$goldLens = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

for ($y = 0; $y -lt $size; $y++) {
    $row = $pattern[$y]

    for ($x = 0; $x -lt $size; $x++) {
        $symbol = [string]$row[$x]

        if ($eyeSymbols -contains $symbol) {
            $symbol = Get-GlassFor $y
        }

        if ($symbol -eq '.') {
            $goldLens.SetPixel($x, $y, $clear)
            continue
        }

        if (-not $colors.ContainsKey($symbol)) {
            throw "Row $y column $x uses '$symbol', which has no colour."
        }

        $goldLens.SetPixel($x, $y, (Convert-Hex $colors[$symbol]))
    }
}

$goldPath = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\gold_lens.png'
$goldLens.Save($goldPath, [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output "saved: $goldPath"

# The single frame the grid preview uses: the eye looking straight ahead, which is
# the resting pose and the one most of the loop spends time on.
$bmp = New-Frame 0


# ---------------------------------------------------------------------------
# Preview. Same idea as make-san-icon.ps1's: a magnified grid with row and column
# numbers over a checkerboard, so a pixel can be pointed at by coordinate, plus
# the sprite at the size it is actually seen in an inventory slot.
# ---------------------------------------------------------------------------

$cell = 26
$margin = 22
$gap = 16
$gridSize = $cell * $size
$slotZoom = 4

# The three distinct poses, in the order left, ahead, right, so the travel can be
# read at a glance in the preview. The strip itself stores them as ahead, left,
# right, which is a different order and deliberately so: index 0 is the resting
# pose, which is what a renderer falls back to.
$previewOrder = @(-1, 0, 1)

$width = [Math]::Max($gap + $margin + $gridSize + $gap, $gap + ($previewOrder.Count + 1) * ($size * $slotZoom + 10) + 20 + $gap)
$height = $gap + 20 + $margin + $gridSize + $gap + 22 + $size * $slotZoom + $gap

$out = New-Object System.Drawing.Bitmap $width, $height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear((Convert-Hex '3A3A3A'))

$font = New-Object System.Drawing.Font 'Consolas', 10
$titleFont = New-Object System.Drawing.Font 'Consolas', 13, ([System.Drawing.FontStyle]::Bold)
$white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$grey = New-Object System.Drawing.SolidBrush ((Convert-Hex '909090'))
$gridPen = New-Object System.Drawing.Pen ((Convert-Hex '505050')), 1
$checkerA = New-Object System.Drawing.SolidBrush ((Convert-Hex '4A4A4A'))
$checkerB = New-Object System.Drawing.SolidBrush ((Convert-Hex '404040'))

$panelTop = $gap + 20
$g.DrawString('san_lens  centre frame  16x16', $titleFont, $white, [single]$gap, [single]($panelTop - 20))

$gx = $gap + $margin
$gy = $panelTop + $margin

for ($x = 0; $x -lt $size; $x++) {
    $g.DrawString([string]$x, $font, $grey, [single]($gx + $x * $cell + $cell / 2 - 7), [single]($panelTop + 4))
}

for ($y = 0; $y -lt $size; $y++) {
    $g.DrawString([string]$y, $font, $grey, [single]($gap + 2), [single]($gy + $y * $cell + $cell / 2 - 8))
}

for ($y = 0; $y -lt $size; $y++) {
    for ($x = 0; $x -lt $size; $x++) {
        $brush = if ((($x + $y) % 2) -eq 0) { $checkerA } else { $checkerB }
        $g.FillRectangle($brush, [int]($gx + $x * $cell), [int]($gy + $y * $cell), [int]$cell, [int]$cell)
    }
}

$g.DrawImage($bmp, (New-Object System.Drawing.Rectangle ([int]$gx), ([int]$gy), ([int]$gridSize), ([int]$gridSize)))

for ($i = 0; $i -le $size; $i++) {
    $g.DrawLine($gridPen, [int]($gx + $i * $cell), [int]$gy, [int]($gx + $i * $cell), [int]($gy + $gridSize))
    $g.DrawLine($gridPen, [int]$gx, [int]($gy + $i * $cell), [int]($gx + $gridSize), [int]($gy + $i * $cell))
}

$slotTop = $gy + $gridSize + $gap + 22
$g.DrawString('left / ahead / right  +  gold_lens, 4x', $titleFont, $white, [single]$gap, [single]($slotTop - 20))

# The three distinct poses at the size a slot draws them, in the order left, ahead,
# right, so the travel can be read at a glance. The strip itself stores them as
# ahead, left, right, which is a different order and deliberately so: index 0 is the
# resting pose, which is what a renderer falls back to.
for ($i = 0; $i -lt $previewOrder.Count; $i++) {
    $preview = New-Frame $previewOrder[$i]
    $left = $gap + $i * ($size * $slotZoom + 10)
    $g.DrawImage($preview, (New-Object System.Drawing.Rectangle ([int]$left), ([int]$slotTop), ([int]($size * $slotZoom)), ([int]($size * $slotZoom))))
    $preview.Dispose()
}

# The Gold Lens after a wider gap, so it is clearly a separate item rather than a
# fourth pose of the same one.
$goldLeft = $gap + $previewOrder.Count * ($size * $slotZoom + 10) + 20
$g.DrawImage($goldLens, (New-Object System.Drawing.Rectangle ([int]$goldLeft), ([int]$slotTop), ([int]($size * $slotZoom)), ([int]($size * $slotZoom))))

$font.Dispose()
$titleFont.Dispose()
$white.Dispose()
$grey.Dispose()
$gridPen.Dispose()
$checkerA.Dispose()
$checkerB.Dispose()
$g.Dispose()
$bmp.Dispose()
$goldLens.Dispose()

New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'build') | Out-Null
$previewPath = Join-Path $PSScriptRoot 'build\lens-icon-preview.png'
$out.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

Write-Output "preview: $previewPath"
