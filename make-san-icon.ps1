Add-Type -AssemblyName System.Drawing

# ============================================================================
#  San 状态图标生成脚本
#      abyssfall:hud/san_empty, san_half, san_full
#
#  跑一下：
#      powershell -NoProfile -ExecutionPolicy Bypass -File .\make-san-icon.ps1
#
#  会做两件事：
#    1. 把三张 9x9 精灵图写进 src\main\resources\...\textures\gui\sprites\hud\
#    2. 在 build\san-icon-preview.png 生成一张带坐标的放大预览（build 不进仓库）
#
#  要改图案或颜色，改下面【图案】和【颜色】两块就行，别的不用动。
#
#  9x9 是游戏定的：1.21.11 的 Gui.renderFood 按 9x9 贴图、间距 8 排十个格子，
#  原版 food_empty/half/full 三张图本身也是 9x9。跟着它才能和饥饿排融为一体。
#
#  这里不写 .mcmeta。gui/sprites/hud 下的图会被 GUI 图集当普通图读取；原版在这个
#  目录里唯一带 mcmeta 的是定位条那几张九宫格拉伸图，我们不是。
#
#  图案是画出来看着改定的，不是推理出来的：一坨正在失去形状、并且甩出一滴的东西。
#  最终图案由 Kainy 定稿。滴落那一点故意和主体断开——在 9 像素的尺度上，连着主体
#  的滴会被读成一根针。
# ============================================================================


# ---------------------------------------------------------------------------
# 【图案】9 行 × 9 列。一个字符 = 一个像素。
#
#   .  = 透明（什么都不画）
#   O  = 黑色轮廓
#   #  = 主体
#   +  = 高光（主体的亮色）
#   -  = 暗部（主体的暗色）
#   D  = 深色（比暗部更深，画滴落用）
#   X  = 黑洞（亮版暗版都是纯黑）
#
# 列号 →  0 1 2 3 4 5 6 7 8
# ---------------------------------------------------------------------------
$pattern = @(
    '..OOOO...',   # 第 0 行
    '.O+##OO..',   # 第 1 行
    'O##X###O.',   # 第 2 行
    'O######OO',   # 第 3 行
    'O#X####-O',   # 第 4 行
    'O######-O',   # 第 5 行
    '.O#O##-O.',   # 第 6 行
    '..O.O#O..',   # 第 7 行
    '.....O...'    # 第 8 行
)


# ---------------------------------------------------------------------------
# 【颜色】亮版 = San 还在的格子。六位十六进制，改数字即可。
#
# 9B6BC9 是项目里已经在用的 San 紫（进度条满值色、深渊探索者图标同色系），
# 所以这个图标不需要另做介绍。高光和暗部是同色相提亮压暗，不是另挑的颜色——
# 这样以后想把整张图往低 San 的红色染，明暗关系不会散掉。
# ---------------------------------------------------------------------------
$litColors = @{
    'O' = '000000'   # 轮廓：纯黑
    'X' = '000000'   # 黑洞：纯黑
    '#' = '9B6BC9'   # 主体：San 紫
    '+' = 'C4A2E3'   # 高光：淡紫
    '-' = '6E4A96'   # 暗部：暗紫
    'D' = '4A2E68'   # 深色：更暗的紫
}

# 【颜色】暗版 = San 已耗尽的空格子。
#
# 用的是原版自己的两个值，从 food_empty.png 里逐像素读出来的：轮廓纯黑，
# 里面一律 282828。空格子挨着饥饿排显示，"什么都没有"的那个灰度应该和状态栏
# 其余部分一致。注意内部没有任何细节——原版空图标也没有，这里要是把黑洞画深了，
# 空格子就变成一个花纹了。
$unlitColors = @{
    'O' = '000000'
    'X' = '000000'
    '#' = '282828'
    '+' = '282828'
    '-' = '282828'
    'D' = '282828'
}

# 【半格】保留左边几列的亮色，右边的列留空，露出下面垫的暗版。
#
# 这是原版的方向，有据可查：把 heart/full.png 和 heart/half.png 逐像素比对，
# 满心第 2 行是 x=1..7 有料，半心是 x=1..4 有料 —— 保留左半，右半透明。
# 也就是说格子是【从右往左】被掏空的，和一条从左往右缩短的进度条一致。
#
# 注意别拿 food_half.png 反推：鸡腿图案本身是倾斜不对称的，它的半格看着像
# “变瘦了”而不是“切掉一半”，很容易把方向读反。对称的心才是可靠证据。
#

# 【颜色】高亮版 = San 刚刚回满时闪的那一下。
#
# 必须另做一张贴图，不能靠代码提亮：blitSprite 的 tint 是【乘算】的，
# 而 ARGB.white(alpha) 的 RGB 本身就是 0xFFFFFF，乘白等于原色 —— 乘算只能变暗，
# 永远无法变亮。原版也是这么干的：它给心备了一张 heart/full_blinking.png，
# 把 FF1313（暗红）提成 FFA1A1（亮粉红），而不是在代码里调亮。
#
# 这里的做法和原版一致：把亮版配色向白提，保留黑轮廓。轮廓不提——
# 原版 full_blinking 的深色边（BB1313→DEA1A1）也只提了一点，轮廓全提会让
# 图标失去形状。
$flashColors = @{
    'O' = '000000'   # 轮廓：不变
    'X' = '000000'   # 黑洞：不变
    '#' = 'E4CDF4'   # 主体：亮紫
    '+' = 'FBF4FF'   # 高光：几乎白
    '-' = 'C9A8E0'   # 暗部
    'D' = 'A87FC4'   # 深色
}
# 值 = 保留的列数。5 表示保留 x=0..4，右边 x=5..8 留空。
$halfKeepColumns = 5


# ===========================  以下不用改  ===========================

function Convert-Hex([string]$hex) {
    $r = [Convert]::ToInt32($hex.Substring(0, 2), 16)
    $g = [Convert]::ToInt32($hex.Substring(2, 2), 16)
    $b = [Convert]::ToInt32($hex.Substring(4, 2), 16)
    return [System.Drawing.Color]::FromArgb(255, $r, $g, $b)
}

$transparent = [System.Drawing.Color]::FromArgb(0, 0, 0, 0)

function New-Sprite([hashtable]$colors, [int]$keepColumns) {
    $bmp = New-Object System.Drawing.Bitmap 9, 9, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

    # 位图内存本来就是清零的，但每个像素都显式写一遍，文件就不依赖这一点。
    for ($y = 0; $y -lt 9; $y++) {
        for ($x = 0; $x -lt 9; $x++) {
            $bmp.SetPixel($x, $y, $transparent)
        }
    }

    for ($y = 0; $y -lt 9; $y++) {
        $row = $pattern[$y]

        for ($x = 0; $x -lt $keepColumns; $x++) {
            $key = [string]$row[$x]

            if ($colors.ContainsKey($key)) {
                $bmp.SetPixel($x, $y, (Convert-Hex $colors[$key]))
            }
        }
    }

    return $bmp
}

$lit = New-Sprite $litColors 9
$unlit = New-Sprite $unlitColors 9
$half = New-Sprite $litColors $halfKeepColumns
$litFlash = New-Sprite $flashColors 9
$halfFlash = New-Sprite $flashColors $halfKeepColumns

# --- 写进 mod 资源目录 ---
$spriteDir = Join-Path $PSScriptRoot 'src\main\resources\assets\abyssfall\textures\gui\sprites\hud'
New-Item -ItemType Directory -Force -Path $spriteDir | Out-Null

$unlit.Save((Join-Path $spriteDir 'san_empty.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$lit.Save((Join-Path $spriteDir 'san_full.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$half.Save((Join-Path $spriteDir 'san_half.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$litFlash.Save((Join-Path $spriteDir 'san_full_blinking.png'), [System.Drawing.Imaging.ImageFormat]::Png)
$halfFlash.Save((Join-Path $spriteDir 'san_half_blinking.png'), [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output "精灵图: $spriteDir"
Write-Output "  san_empty.png / san_full.png / san_half.png
  san_full_blinking.png / san_half_blinking.png"

# --- 预览图：上面三个放大格（带坐标），下面一行十格模拟 HUD ---

$cell = 26          # 放大后每个像素多大
$gridSize = 9 * $cell
$margin = 22        # 留给坐标数字
$gap = 18
$rowZoom = 5

$panelWidth = $margin + $gridSize
$rowWidth = (9 + 9 * 8) * $rowZoom
$width = [Math]::Max($gap + ($panelWidth + $gap) * 3, $gap * 2 + $rowWidth)
$height = $gap + 20 + $margin + $gridSize + $gap + 22 + 9 * $rowZoom + $gap

$out = New-Object System.Drawing.Bitmap $width, $height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear((Convert-Hex '3A3A3A'))

$font = New-Object System.Drawing.Font 'Consolas', 11
$titleFont = New-Object System.Drawing.Font 'Consolas', 13, ([System.Drawing.FontStyle]::Bold)
$white = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
$grey = New-Object System.Drawing.SolidBrush ((Convert-Hex '909090'))
$gridPen = New-Object System.Drawing.Pen ((Convert-Hex '505050')), 1
$checkerA = New-Object System.Drawing.SolidBrush ((Convert-Hex '4A4A4A'))
$checkerB = New-Object System.Drawing.SolidBrush ((Convert-Hex '404040'))

function Draw-Panel([System.Drawing.Bitmap]$sprite, [int]$left, [int]$top, [string]$title) {
    $g.DrawString($title, $titleFont, $white, [single]$left, [single]($top - 20))

    $gx = $left + $margin
    $gy = $top + $margin

    for ($x = 0; $x -lt 9; $x++) {
        $g.DrawString([string]$x, $font, $grey, [single]($gx + $x * $cell + $cell / 2 - 5), [single]($top + 2))
    }

    for ($y = 0; $y -lt 9; $y++) {
        $g.DrawString([string]$y, $font, $grey, [single]($left + 4), [single]($gy + $y * $cell + $cell / 2 - 8))
    }

    # 棋盘格，好看出哪些像素是透明的
    for ($y = 0; $y -lt 9; $y++) {
        for ($x = 0; $x -lt 9; $x++) {
            $brush = if ((($x + $y) % 2) -eq 0) { $checkerA } else { $checkerB }
            $g.FillRectangle($brush, [int]($gx + $x * $cell), [int]($gy + $y * $cell), [int]$cell, [int]$cell)
        }
    }

    $g.DrawImage($sprite, (New-Object System.Drawing.Rectangle ([int]$gx), ([int]$gy), ([int]$gridSize), ([int]$gridSize)))

    for ($i = 0; $i -le 9; $i++) {
        $g.DrawLine($gridPen, [int]($gx + $i * $cell), [int]$gy, [int]($gx + $i * $cell), [int]($gy + $gridSize))
        $g.DrawLine($gridPen, [int]$gx, [int]($gy + $i * $cell), [int]($gx + $gridSize), [int]($gy + $i * $cell))
    }
}

$panelTop = $gap + 20

Draw-Panel $lit $gap $panelTop 'lit  (San 还在)'
Draw-Panel $unlit ($gap + $panelWidth + $gap) $panelTop 'unlit  (空格子)'
Draw-Panel $half ($gap + ($panelWidth + $gap) * 2) $panelTop 'half  (半格)'

$rowTop = $panelTop + $margin + $gridSize + $gap + 22
$g.DrawString('HUD 实际大小，十格，55%', $titleFont, $white, [single]$gap, [single]($rowTop - 20))

for ($i = 0; $i -lt 10; $i++) {
    $rect = New-Object System.Drawing.Rectangle ([int]($gap + $i * 8 * $rowZoom)), ([int]$rowTop), ([int](9 * $rowZoom)), ([int](9 * $rowZoom))

    $g.DrawImage($unlit, $rect)

    if (($i * 2 + 1) -lt 11) {
        $g.DrawImage($lit, $rect)
    }

    if (($i * 2 + 1) -eq 11) {
        $g.DrawImage($half, $rect)
    }
}

$font.Dispose()
$titleFont.Dispose()
$white.Dispose()
$grey.Dispose()
$gridPen.Dispose()
$checkerA.Dispose()
$checkerB.Dispose()
$g.Dispose()

New-Item -ItemType Directory -Force -Path (Join-Path $PSScriptRoot 'build') | Out-Null
$previewPath = Join-Path $PSScriptRoot 'build\san-icon-preview.png'
$out.Save($previewPath, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose()

$litFlash.Dispose()
$halfFlash.Dispose()
$lit.Dispose()
$unlit.Dispose()
$half.Dispose()

Write-Output "预览图: $previewPath"

