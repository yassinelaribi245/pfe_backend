# Download Git for Windows
Write-Host "Downloading Git installer..."
$url = "https://github.com/git-for-windows/git/releases/download/v2.43.0.windows.1/Git-2.43.0-64-bit.exe"
$output = "Git-2.43.0-64-bit.exe"

try {
    Invoke-WebRequest -Uri $url -OutFile $output
    Write-Host "Download completed successfully!"
    Write-Host "Run Git-2.43.0-64-bit.exe to install Git"
    Write-Host "After installation, restart your IDE and run the git commands"
} catch {
    Write-Host "Download failed. Please download manually from: https://git-scm.com/download/win"
}
