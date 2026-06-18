$env:ANDROID_HOME = "C:\Users\abinm\AppData\Local\Android\Sdk"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"

Write-Host "=== Java ==="
& "$env:JAVA_HOME\bin\java.exe" -version

Write-Host ""
Write-Host "=== Building KTMB Crowd & Trend ==="
Set-Location "C:\Users\abinm\OneDrive\Documents\01_Active_Work\Business_Projects\ktmb_dashboards\android"

Write-Host "Working dir: $(Get-Location)"
Write-Host "gradlew.bat exists: $(Test-Path .\gradlew.bat)"

& "C:\Users\abinm\OneDrive\Documents\01_Active_Work\Business_Projects\ktmb_dashboards\android\gradlew.bat" clean assembleDebug --no-daemon 2>&1

Write-Host ""
Write-Host "=== Exit code: $LASTEXITCODE ==="
if ($LASTEXITCODE -eq 0) {
    Write-Host "APK built successfully!"
    Get-ChildItem -Recurse app\build\outputs\apk\debug\*.apk | Select-Object FullName
} else {
    Write-Host "Build failed. Check errors above."
}
