# PowerShell script to download a simple beep sound
# Run this script to download a tick sound file

Write-Host "Downloading tick sound file..." -ForegroundColor Green

# Create a simple beep sound using .NET
Add-Type -AssemblyName System.Windows.Forms

# Generate a simple beep
[System.Console]::Beep(800, 200)

Write-Host "Beep sound generated!" -ForegroundColor Green
Write-Host "Note: This is just a test beep. For the actual app, you need to:" -ForegroundColor Yellow
Write-Host "1. Download a short MP3 file (less than 500ms)" -ForegroundColor Yellow
Write-Host "2. Name it 'tick.mp3'" -ForegroundColor Yellow
Write-Host "3. Place it in 'app/src/main/res/raw/'" -ForegroundColor Yellow
Write-Host "4. Replace the current placeholder file" -ForegroundColor Yellow

Write-Host "`nRecommended sources:" -ForegroundColor Cyan
Write-Host "- https://freesound.org/ (search for 'beep' or 'tick')" -ForegroundColor White
Write-Host "- https://www.zapsplat.com/ (search for 'notification beep')" -ForegroundColor White
Write-Host "- https://mixkit.co/free-sound-effects/beep/" -ForegroundColor White

Write-Host "`nPress any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") 