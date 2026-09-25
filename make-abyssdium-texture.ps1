# Generates the Abyssdium item texture: a placeholder abyss-element crystal shard.
#
# PLACEHOLDER ART
# ---------------
# Drawn in code as a stand-in until real artwork exists. The map below IS the picture:
# one character per pixel, '.' transparent, anything else a palette entry.
#
# ALPHA IS BINARY BY CONSTRUCTION
# -------------------------------
# Every pixel is written either fully opaque or fully transparent, because item geometry
# is extruded from the texture's silhouette and SpriteContents.isTransparent is a strict
# `alpha == 0` test -- a semi-transparent pixel grows side faces nobody can see. See
# make-death-omen-texture.ps1 and REFERENCE.md 18i for the full story.

Add-Type -AssemblyName System.Drawing

# Palette keys must be case-insensitively distinct: PowerShell hashtables ignore case,
# so 'O' and 'o' would collide.
$palette = @{
    'X' = [System.Drawing.Color]::FromArgb(255, 14, 14, 22)      # edge: near-black
    'L' = [System.Drawing.Color]::FromArgb(255, 51, 51, 79)      # body, lit side
    'd' = [System.Drawing.Color]::FromArgb(255, 35, 35, 53)      # body, shaded side
    '*' = [System.Drawing.Color]::FromArgb(255, 106, 106, 148)   # upper facet
    '+' = [System.Drawing.Color]::FromArgb(255, 168, 168, 204)   # glint
}

$rows = @(
    '................',
    '......XX........',
    '.....X**X.......',
    '....X**LdX......',
    '...X**LLddX.....',
    '...X*LLLddX.....',
    '..X*LLLLLddX....',
    '..XLLLLLL+dX....',
    '..XLLLLLdddX....',
    '..XLLLLLdddX....',
    '...XLLLLddX.....',
    '...XLLLddX......',
    '....XLLddX......',
    '.....XddX.......',
    '......XX........',
    '................'
)

if ($rows.Count -ne 16) {
    throw "Expected 16 rows, found $($rows.Count)"
}

$bmp    = New-Object System.Drawing.Bitmap(16, 16)
$opaque = 0

for ($y = 0; $y -lt 16; $y++) {
    if ($rows[$y].Length -ne 16) {
        throw "Row $y is $($rows[$y].Length) wide, expected 16"
    }

    for ($x = 0; $x -lt 16; $x++) {
        $key = [string] $rows[$y][$x]

        if ($key -eq '.') {
            # Fully transparent, and colourless with it: a stray colour behind zero alpha
            # still shows up in some tooling and in the particle sprite picker.
            $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0))
        } else {
            if (-not $palette.ContainsKey($key)) {
                throw "Unknown palette entry '$key' at ($x, $y)"
            }
            $bmp.SetPixel($x, $y, $palette[$key])
            $opaque++
        }
    }
}

$out = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\item\abyssdium.png'
New-Item -ItemType Directory -Force -Path (Split-Path $out) | Out-Null
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

Write-Output "Opaque pixels:      $opaque"
Write-Output "Transparent pixels: $(256 - $opaque)"
Write-Output "Wrote $out"
