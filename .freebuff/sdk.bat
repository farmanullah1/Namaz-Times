@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot"
set "ANDROID_HOME=%~dp0..\.android-sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
call "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" --sdk_root="%ANDROID_HOME%" %*
