#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SDK_DIR=${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}
if [ -z "$SDK_DIR" ]; then
  for candidate in "$HOME/Android/Sdk" "$HOME/Library/Android/sdk"; do
    if [ -d "$candidate" ]; then
      SDK_DIR=$candidate
      break
    fi
  done
fi

if [ -z "$SDK_DIR" ] || [ ! -d "$SDK_DIR" ]; then
  printf '%s\n' "Set ANDROID_SDK_ROOT or ANDROID_HOME to your Android SDK path." >&2
  exit 1
fi

BUILD_TOOLS=${ANDROID_BUILD_TOOLS:-$SDK_DIR/build-tools/35.0.0}
PLATFORM_JAR=${ANDROID_PLATFORM_JAR:-$SDK_DIR/platforms/android-33/android.jar}
if [ -n "${JAVA_HOME:-}" ]; then
  PATH=$JAVA_HOME/bin:$PATH
fi
PATH=$BUILD_TOOLS:$PATH

APP_ID=dk.fez.bluetoothquickconnect
VERSION_CODE=1
VERSION_NAME=0.1.0
MIN_SDK=23
TARGET_SDK=33
OUT_DIR=$ROOT_DIR/build
GEN_DIR=$OUT_DIR/gen
CLASSES_DIR=$OUT_DIR/classes
DEX_DIR=$OUT_DIR/dex
MANIFEST=$OUT_DIR/AndroidManifest.xml
CLASSES_JAR=$OUT_DIR/classes.jar
RES_ZIP=$OUT_DIR/resources.zip
UNALIGNED_APK=$OUT_DIR/bluetooth-quick-connect-unsigned.apk
ALIGNED_APK=$OUT_DIR/bluetooth-quick-connect-aligned.apk
SIGNED_APK=$OUT_DIR/bluetooth-quick-connect.apk
KEYSTORE=$OUT_DIR/debug.keystore

rm -rf "$GEN_DIR" "$CLASSES_DIR" "$DEX_DIR" "$MANIFEST" "$CLASSES_JAR" "$RES_ZIP" "$UNALIGNED_APK" "$ALIGNED_APK" "$SIGNED_APK" "$SIGNED_APK.idsig"
rm -f "$OUT_DIR"/*.apk "$OUT_DIR"/*.apk.idsig
mkdir -p "$OUT_DIR" "$GEN_DIR" "$CLASSES_DIR" "$DEX_DIR"

sed "s|<manifest |<manifest package=\"$APP_ID\" |" "$ROOT_DIR/app/src/main/AndroidManifest.xml" > "$MANIFEST"

aapt2 compile --dir "$ROOT_DIR/app/src/main/res" -o "$OUT_DIR/compiled-res.zip"
aapt2 link \
  -I "$PLATFORM_JAR" \
  --manifest "$MANIFEST" \
  --java "$GEN_DIR" \
  --custom-package "$APP_ID" \
  --rename-manifest-package "$APP_ID" \
  --min-sdk-version "$MIN_SDK" \
  --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME" \
  --auto-add-overlay \
  -o "$RES_ZIP" \
  "$OUT_DIR/compiled-res.zip"

javac -source 8 -target 8 \
  -bootclasspath "$PLATFORM_JAR" \
  -d "$CLASSES_DIR" \
  "$ROOT_DIR/app/src/main/java/dk/fez/bluetoothquickconnect/"*.java \
  "$GEN_DIR/dk/fez/bluetoothquickconnect/R.java"

jar cf "$CLASSES_JAR" -C "$CLASSES_DIR" .
d8 --min-api 23 --lib "$PLATFORM_JAR" --output "$DEX_DIR" "$CLASSES_JAR"
cp "$RES_ZIP" "$UNALIGNED_APK"
cd "$DEX_DIR"
zip -q "$UNALIGNED_APK" classes.dex
cd "$ROOT_DIR"

zipalign -f 4 "$UNALIGNED_APK" "$ALIGNED_APK"

if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair \
    -keystore "$KEYSTORE" \
    -storepass android \
    -keypass android \
    -alias androiddebugkey \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

apksigner sign \
  --ks "$KEYSTORE" \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$SIGNED_APK" \
  "$ALIGNED_APK"

apksigner verify "$SIGNED_APK"
printf '%s\n' "$SIGNED_APK"
