@echo off
set ANDROID_HOME=C:\Users\abinm\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%ANDROID_HOME%\platform-tools;%PATH%

echo === Java ===
java -version

echo.
echo === SDK platforms ===
dir "%ANDROID_HOME%\platforms"

echo.
echo === Building KTMB Crowd & Trend ===
echo.

cd /d C:\Users\abinm\OneDrive\Documents\01_Active_Work\Business_Projects\ktmb_dashboards\android

call gradlew.bat clean assembleDebug --no-daemon --stacktrace 2>&1

echo.
echo === Build exit code: %ERRORLEVEL% ===
if %ERRORLEVEL% EQU 0 (
    echo.
    echo APK built successfully!
    dir /s /b app\build\outputs\apk\debug\*.apk
) else (
    echo Build failed. Check errors above.
)
