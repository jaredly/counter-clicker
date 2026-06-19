#!/usr/bin/env bash
set -euo pipefail

VERSION="$(node -p "require('./package.json').version")"
TAG="v${VERSION}"
APK="app/build/outputs/apk/debug/app-debug.apk"

gradle :app:assembleDebug

if gh release view "${TAG}" >/dev/null 2>&1; then
  gh release upload "${TAG}" "${APK}" --clobber
else
  gh release create "${TAG}" "${APK}" \
    --title "${TAG}" \
    --notes "Debug APK for ${TAG}."
fi
