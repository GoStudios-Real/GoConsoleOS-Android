# GoConsoleOS Android â€” Aptoide store listing package
# Generates store assets (icons + feature graphics) for every companion app.
# Usage:  powershell -File generate-assets.ps1
# Output: ./aptoide/<app>/icon.png, ./aptoide/<app>/feature.png

Add-Type -AssemblyName System.Drawing

$ErrorActionPreference = "Stop"

function New-RoundedRectPath {
    param([int]$w, [int]$h, [int]$r)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc(0, 0, $d, $d, 180, 90)
    $p.AddArc($w - $d, 0, $d, $d, 270, 90)
    $p.AddArc($w - $d, $h - $d, $d, $d, 0, 90)
    $p.AddArc(0, $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

function Draw-MonoIcon {
    param(
        [string]$OutPath,
        [int]$Size,
        [string]$BgTop,
        [string]$BgBottom,
        [string]$Glyph,
        [string]$accentVar
    )
    $bmp = New-Object System.Drawing.Bitmap($Size, $Size)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $rect = New-Object System.Drawing.Rectangle(0, 0, $Size, $Size)
    $path = New-RoundedRectPath -w $Size -h $Size -r ([int]($Size * 0.22))

    $c1 = [System.Drawing.Color]::FromArgb(0xFF, 10, 15, 30)
    $c2 = [System.Drawing.Color]::FromArgb(0xFF, 0, 30, 50)
    if ($BgTop) { $c1 = [System.Drawing.ColorTranslator]::FromHtml($BgTop) }
    if ($BgBottom) { $c2 = [System.Drawing.ColorTranslator]::FromHtml($BgBottom) }

    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect, $c1, $c2, 60)
    $g.FillPath($brush, $path)

    # accent glyph pill
    $fs = [float]($Size * 0.42)
    $font = New-Object System.Drawing.Font("Segoe UI", $fs, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $accentBrushColor = [System.Drawing.ColorTranslator]::FromHtml($accentVar)
    $pill = New-Object -TypeName System.Drawing.SolidBrush -ArgumentList $accentBrushColor
    $frmt = New-Object System.Drawing.StringFormat
    $frmt.Alignment = [System.Drawing.StringAlignment]::Center
    $frmt.LineAlignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($Glyph, $font, $pill, ([System.Drawing.RectangleF]$rect), $frmt)

    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(60, 255, 255, 255), 4)
    $g.DrawPath($pen, $path)

    $bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
}

function Draw-Feature {
    param(
        [string]$OutPath,
        [int]$W,
        [int]$H,
        [string]$BgTop,
        [string]$BgBottom,
        [string]$Title,
        [string]$Subtitle,
        [string]$accentVar
    )
    $bmp = New-Object System.Drawing.Bitmap($W, $H)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

    $rect = New-Object System.Drawing.Rectangle(0, 0, $W, $H)
    $c1 = [System.Drawing.Color]::FromArgb(0xFF, 10, 15, 30)
    $c2 = [System.Drawing.Color]::FromArgb(0xFF, 0, 30, 50)
    if ($BgTop) { $c1 = [System.Drawing.ColorTranslator]::FromHtml($BgTop) }
    if ($BgBottom) { $c2 = [System.Drawing.ColorTranslator]::FromHtml($BgBottom) }

    $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $rect, $c1, $c2, -45)
    $g.FillRectangle($brush, $rect)

    foreach ($i in 0..4) {
        $accentDim = [System.Drawing.Color]::FromArgb(18, 0, 201, 219)
        $cx = $W * (0.15 + 0.22 * $i)
        $cy = $H * (0.15 + 0.28 * ($i % 2))
        $r = [int]($H * 0.22)
        $ell = New-Object -TypeName System.Drawing.SolidBrush -ArgumentList $accentDim
        $g.FillEllipse($ell, $cx - ($r / 2), $cy - ($r / 2), $r, $r)
    }

    $accentBrushColor = [System.Drawing.ColorTranslator]::FromHtml($accentVar)
    $tFont = New-Object System.Drawing.Font("Segoe UI", [float]($H * 0.11), [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $sFont = New-Object System.Drawing.Font("Segoe UI", [float]($H * 0.045), [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
    $tBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(0xFF, 232, 232, 245))
    $sBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(0xFF, 122, 128, 160))
    $aBrush = New-Object -TypeName System.Drawing.SolidBrush -ArgumentList $accentBrushColor

$tf = New-Object System.Drawing.StringFormat
    $tf.Alignment = [System.Drawing.StringAlignment]::Center
    $tf.LineAlignment = [System.Drawing.StringAlignment]::Center
    $titleRect = [System.Drawing.RectangleF]::new([float]0, [float]($H * 0.58), [float]$W, [float]($H * 0.16))
    $subRect = [System.Drawing.RectangleF]::new([float]0, [float]($H * 0.72), [float]$W, [float]($H * 0.09))
    $g.DrawString($Title, $tFont, $tBrush, $titleRect, $tf)
    $g.DrawString($Subtitle, $sFont, $sBrush, $subRect, $tf)

    # accent bar
    $bar = New-Object -TypeName System.Drawing.SolidBrush -ArgumentList $accentBrushColor
    $barRect = [System.Drawing.RectangleF]::new([float](($W/2) - ($W*0.12)), [float]($H * 0.66), [float]($W * 0.24), [float]6)
    $g.FillRectangle($bar, $barRect)

    $bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
}

function New-Listing {
    param($AppId, $Title, $Subtitle, $Glyph, $accentVar, $BgTop, $BgBottom)
    $dir = Join-Path $PSScriptRoot "aptoide\$AppId"
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
    Draw-MonoIcon -OutPath (Join-Path $dir "icon.png") -Size 512 -BgTop $BgTop -BgBottom $BgBottom -Glyph $Glyph -accentVar $accentVar
    Draw-MonoIcon -OutPath (Join-Path $dir "icon-192.png") -Size 192 -BgTop $BgTop -BgBottom $BgBottom -Glyph $Glyph -accentVar $accentVar
    Draw-MonoIcon -OutPath (Join-Path $dir "icon-48.png") -Size 48 -BgTop $BgTop -BgBottom $BgBottom -Glyph $Glyph -accentVar $accentVar
    Draw-Feature -OutPath (Join-Path $dir "feature.png") -W 1024 -H 500 -BgTop $BgTop -BgBottom $BgBottom -Title $Title -Subtitle $Subtitle -accentVar $accentVar
    return $dir
}

$cyan  = "#00C9DB"
$blue  = "#3D7BFF"
$green = "#2EEA8F"
$purple = "#8F6BFF"
$orange = "#FF9F43"

New-Listing -AppId "GoConsoleOS-Portable" -Title "GoConsoleOS Portable" -Subtitle "Find and launch your USB game console from anywhere on Wi-Fi" -Glyph "G" -accentVar $cyan -BgTop "#0A0F1E" -BgBottom "#00405A"
New-Listing -AppId "GoConsoleOS-Link" -Title "GoConsoleOS Link" -Subtitle "Browse and launch your host's game library over LAN" -Glyph "L" -accentVar $blue -BgTop "#0A0F1E" -BgBottom "#102A66"
New-Listing -AppId "GoConsoleOS-Cast" -Title "GoConsoleOS Cast" -Subtitle "Mirror your console to a TV or device" -Glyph "C" -accentVar $purple -BgTop "#0A0F1E" -BgBottom "#2E1560"
New-Listing -AppId "GoConsoleOS-USB-Health" -Title "USB Health" -Subtitle "SMART reports for every portable USB console" -Glyph "H" -accentVar $green -BgTop "#0A0F1E" -BgBottom "#0E4A2A"
New-Listing -AppId "GoConsoleOS-GoAI" -Title "GoAI" -Subtitle "Your gaming assistant, fully offline" -Glyph "A" -accentVar $orange -BgTop "#0A0F1E" -BgBottom "#553002"

Write-Output "Done. Assets written under $PSScriptRoot\aptoide"




