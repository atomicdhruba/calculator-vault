# ==============================================================================
# Calculator Stealth Vault - Release Build & Verification Automation
# ==============================================================================
# Usage: powershell -ExecutionPolicy Bypass -File scripts\build_release.ps1
# ==============================================================================

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path

Write-Host "================================================================"
Write-Host " Calculator Stealth Vault - Automated Release Build Pipeline"
Write-Host "================================================================"
Write-Host "[i] Script Directory : $scriptDir"
Write-Host "[i] Project Root     : $projectRoot"

$gradlew = Join-Path $projectRoot "gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "[!] Error: gradlew.bat not found at $projectRoot" -ForegroundColor Red
    exit 1
}

$exitCode = 0

Push-Location $projectRoot
try {
    # --------------------------------------------------------------------------
    # Step 1: Clean build artifacts
    # --------------------------------------------------------------------------
    Write-Host ""
    Write-Host "[*] Step 1/3: Cleaning prior build artifacts..."
    & $gradlew clean
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[!] Error: 'gradlew clean' failed with exit code $LASTEXITCODE" -ForegroundColor Red
        $exitCode = $LASTEXITCODE
        return
    }
    Write-Host "[OK] Clean complete." -ForegroundColor Green

    # --------------------------------------------------------------------------
    # Step 2: Run unit test suite
    # --------------------------------------------------------------------------
    Write-Host ""
    Write-Host "[*] Step 2/3: Executing unit tests (testDebugUnitTest)..."
    & $gradlew testDebugUnitTest
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[!] Error: Unit tests failed with exit code $LASTEXITCODE" -ForegroundColor Red
        $exitCode = $LASTEXITCODE
        return
    }
    Write-Host "[OK] All unit tests passed successfully." -ForegroundColor Green

    # --------------------------------------------------------------------------
    # Step 3: Compile and assemble release APK with R8 minification & shrinking
    # --------------------------------------------------------------------------
    Write-Host ""
    Write-Host "[*] Step 3/3: Assembling release build with R8 obfuscation..."
    & $gradlew assembleRelease
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[!] Error: 'gradlew assembleRelease' failed with exit code $LASTEXITCODE" -ForegroundColor Red
        $exitCode = $LASTEXITCODE
        return
    }
    Write-Host "[OK] Release build assembled successfully." -ForegroundColor Green

    # --------------------------------------------------------------------------
    # Step 4: Verify Release APK artifact & generate checksum
    # --------------------------------------------------------------------------
    $releaseApkDir = Join-Path $projectRoot "app\build\outputs\apk\release"
    $targetApk = Join-Path $releaseApkDir "app-release.apk"

    if (-not (Test-Path $targetApk)) {
        # Fallback search for any .apk in release directory
        $matchingApks = Get-ChildItem -Path $releaseApkDir -Filter "*.apk" -ErrorAction SilentlyContinue
        if ($matchingApks -and ($matchingApks.Count -gt 0)) {
            $targetApk = $matchingApks[0].FullName
        } else {
            Write-Host "[!] Error: No release APK found in $releaseApkDir" -ForegroundColor Red
            $exitCode = 1
            return
        }
    }

    $apkItem = Get-Item $targetApk
    $apkBytes = $apkItem.Length
    $apkKb = [Math]::Round($apkBytes / 1KB, 2)
    $apkMb = [Math]::Round($apkBytes / 1MB, 2)
    $hashObj = Get-FileHash -Path $apkItem.FullName -Algorithm SHA256
    $sha256 = $hashObj.Hash

    Write-Host ""
    Write-Host "================================================================"
    Write-Host " [OK] RELEASE BUILD ARTIFACT VERIFIED"
    Write-Host "================================================================"
    Write-Host "[+] Path    : $($apkItem.FullName)"
    Write-Host "[+] Size    : $apkMb MB ($apkKb KB / $apkBytes bytes)"
    Write-Host "[+] SHA-256 : $sha256"
    Write-Host "================================================================"
    Write-Host ""
}
catch {
    Write-Host "[!] Unhandled build exception: $_" -ForegroundColor Red
    $exitCode = 1
}
finally {
    Pop-Location
}

exit $exitCode
