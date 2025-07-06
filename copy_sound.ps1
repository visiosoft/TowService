# PowerShell script to copy tick.mp3 from Downloads to the raw folder

Write-Host "Copying tick.mp3 from Downloads..." -ForegroundColor Green

try {
    # Check if the source file exists
    if (Test-Path "Downloads/tick.mp3") {
        # Copy the file to the raw folder
        Copy-Item "Downloads/tick.mp3" "app/src/main/res/raw/tick.mp3" -Force
        Write-Host "Successfully copied tick.mp3 to app/src/main/res/raw/" -ForegroundColor Green
        Write-Host "The countdown timer will now use your sound file!" -ForegroundColor Cyan
    } else {
        Write-Host "Error: tick.mp3 not found in Downloads folder" -ForegroundColor Red
        Write-Host "Please make sure the file exists at: Downloads/tick.mp3" -ForegroundColor Yellow
    }
} catch {
    Write-Host "Error copying file: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown") 