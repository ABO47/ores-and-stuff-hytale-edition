param(
    [string]$ModsDir = "D:\Games\hytale game\game\UserData\Mods"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = $PSScriptRoot
if (-not $ProjectRoot) { $ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path }
Set-Location $ProjectRoot

# 1. Build
$env:JAVA_HOME = "C:\Users\Abdullah47\.jdks\temurin-25.0.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "Building mod..." -ForegroundColor Cyan
& .\gradlew build -x test
if ($LASTEXITCODE -ne 0) { Write-Error "Build failed"; exit $LASTEXITCODE }
Write-Host "Build OK" -ForegroundColor Green

# 2. Find built jar
$jar = Get-ChildItem -Path "$ProjectRoot\build\libs" -Filter "*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jar) { Write-Error "No jar found in build/libs"; exit 1 }
Write-Host "Built: $($jar.Name)" -ForegroundColor Green

# 3. Ensure Mods dir exists
if (-not (Test-Path $ModsDir)) { New-Item -ItemType Directory -Path $ModsDir -Force | Out-Null }

# 4. Remove duplicates (any previous version of this mod)
#    Matches: "ores*stuff*.jar" and the old "oresandstuff*.jar" / "HytaleModding*" variants
$patterns = @("*ores*stuff*.jar", "*oresandstuff*.jar")
$removed = @()
foreach ($pat in $patterns) {
    Get-ChildItem -Path $ModsDir -Filter $pat -File -ErrorAction SilentlyContinue | ForEach-Object {
        # Keep the exact file we're about to deploy if it already exists with same name (will be overwritten)
        if ($_.Name -eq $jar.Name) { return }
        Write-Host "Removing duplicate: $($_.Name)" -ForegroundColor Yellow
        Remove-Item -LiteralPath $_.FullName -Force
        $removed += $_.Name
    }
}
# Also remove exact-name old copy to avoid locked-file weirdness (will be overwritten below, but remove first)
$exactOld = Join-Path $ModsDir $jar.Name
$exactRemoved = $false
if (Test-Path $exactOld) {
    if ($removed -notcontains $jar.Name) { Write-Host "Removing old: $($jar.Name)" -ForegroundColor Yellow; $exactRemoved = $true }
    Remove-Item -LiteralPath $exactOld -Force -ErrorAction SilentlyContinue
}
if ($removed.Count -eq 0 -and -not $exactRemoved) { Write-Host "No duplicates found" -ForegroundColor DarkGray }

# 5. Deploy
$dest = Join-Path $ModsDir $jar.Name
Copy-Item -LiteralPath $jar.FullName -Destination $dest -Force
Write-Host "Deployed -> $dest" -ForegroundColor Green
Get-Item $dest | Format-List Name,Length,LastWriteTime | Out-String | Write-Host
