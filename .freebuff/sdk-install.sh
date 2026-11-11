#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SDK="$ROOT/.android-sdk"
LOG="$SDK/install.log"
mkdir -p "$SDK"
exec > >(tee -a "$LOG") 2>&1

echo "[sdk] root: $ROOT"
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager.bat" ] && [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  echo "[sdk] downloading command-line tools"
  curl -L --fail --retry 3 -o "$SDK/cmdline-tools.zip" \
    https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip
  rm -rf "$SDK/cmdline-tools-tmp"
  mkdir -p "$SDK/cmdline-tools-tmp"
  unzip -q "$SDK/cmdline-tools.zip" -d "$SDK/cmdline-tools-tmp"
  rm -rf "$SDK/cmdline-tools/latest"
  mkdir -p "$SDK/cmdline-tools"
  mv "$SDK/cmdline-tools-tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$SDK/cmdline-tools-tmp" "$SDK/cmdline-tools.zip"
fi

export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager.bat"
[ -x "$SDKMANAGER" ] || SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"

echo "[sdk] accepting licenses"
yes | "$SDKMANAGER" --licenses >/dev/null || true

echo "[sdk] installing packages"
"$SDKMANAGER" --install \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "platforms;android-36.1" || \
"$SDKMANAGER" --install \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"

echo "[sdk] installed packages:"
"$SDKMANAGER" --list_installed || true
echo "[sdk] DONE"
