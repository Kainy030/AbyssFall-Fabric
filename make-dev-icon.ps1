Add-Type -AssemblyName System.Drawing

# Item texture for the developer inventory's tab icon (abyssfall:abyss_dev_icon).
#
# Unlike the mod's other artwork, this one is drawn pixel by pixel rather than as
# vector geometry that is supersampled and filtered down. The whole point of the
# icon is three legible letters at 16x16, and antialiasing at that size turns a
# 1px stroke into a grey smear. Every pixel here is therefore either fully opaque
# black or fully transparent, with nothing in between and no decoration around the
# letters.

$size = 16

# 4x5 glyphs for D and E, and a 5x5 V. Five rows is the smallest height at which
# all three stay unambiguous. '#' is a black pixel, anything else is left
# transparent.
#
# The V is one column wider than its neighbours on purpose. An even-width glyph has
# no centre column, so its lowest row is necessarily two pixels wide and the letter
# reads as a U. Five columns give the strokes a single pixel to converge on, which
# is what makes it a V.
$glyphs = @(
    # D
    @(
        '###.',
        '#..#',
        '#..#',
        '#..#',
        '###.'
    ),
    # E
    @(
        '####',
        '#...',
        '###.',
        '#...',
        '####'
    ),
    # V
    @(
        '#...#',
        '#...#',
        '.#.#.',
        '.#.#.',
        '..#..'
    )
)

$glyphHeight = 5
$spacing     = 1        # blank column between letters

# Widths are read from the glyphs themselves rather than assumed, so the V's extra
# column needs no special handling here.
$glyphWidths = $glyphs | ForEach-Object { $_[0].Length }

# 4 + 1 + 4 + 1 + 5 = 15.
$textWidth = ($glyphWidths | Measure-Object -Sum).Sum + (($glyphs.Count - 1) * $spacing)

# Centred as evenly as an odd remainder allows. The spare column is left on the
# right: D's left edge is a full-height stroke whereas the V's right edge only
# reaches its top two rows, so the drawn mass sits left of centre and giving the
# gap to the right balances it.
$originX = [int][Math]::Floor(($size - $textWidth) / 2)
$originY = [int][Math]::Floor(($size - $glyphHeight) / 2)

$bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

$black = [System.Drawing.Color]::FromArgb(255, 0, 0, 0)
$clear = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

# Bitmap memory starts zeroed, but being explicit means the file never depends on
# that: every pixel outside the letters is written as fully transparent.
for ($y = 0; $y -lt $size; $y++) {
    for ($x = 0; $x -lt $size; $x++) {
        $bmp.SetPixel($x, $y, $clear)
    }
}

$penX = $originX

foreach ($glyph in $glyphs) {
    $width = $glyph[0].Length

    for ($row = 0; $row -lt $glyphHeight; $row++) {
        $line = $glyph[$row]

        for ($col = 0; $col -lt $width; $col++) {
            if ($line[$col] -eq '#') {
                $bmp.SetPixel($penX + $col, $originY + $row, $black)
            }
        }
    }

    $penX += $width + $spacing
}

$path = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\abyss_dev_icon.png'
New-Item -ItemType Directory -Force -Path (Split-Path $path) | Out-Null
$bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output "saved: $path"
