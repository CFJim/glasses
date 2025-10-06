# ClearFrameView (Blade)
- Namespace: com.clearframe.clearframeview
- UDP listener on port 5005, CFVX-Lite binary format

## Build in Android Studio
Build > Build APK(s)  (requires SDK 34, JDK 17)

## PowerShell build
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew clean :app:assembleDebug

## Install
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$apk=(Get-ChildItem -Recurse -Include *.apk -File . | Sort-Object LastWriteTime -Descending | Select -First 1).FullName
& $adb install -r "$apk"
