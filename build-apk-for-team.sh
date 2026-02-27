#!/usr/bin/env bash
# Build a self-signed release APK for internal team distribution (no keystore required).
# Output: apks/ folder and the APK path printed at the end.

set -e

mkdir -p apks
rm -f apks/*.apk

./gradlew :collect_app:assembleSelfSignedRelease

# Copy APK(s); variant may produce one or more (e.g. if splits exist)
cp -v collect_app/build/outputs/apk/selfSignedRelease/*.apk apks/

echo ""
echo "Done. Share the APK(s) from: $(pwd)/apks/"
ls -la apks/
