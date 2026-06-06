# Bluetooth Quick Connect for Android TV

Bluetooth Quick Connect is an Android TV and Google TV utility for reconnecting paired Bluetooth headphones, Bluetooth headsets, speakers, and other A2DP audio devices without digging through TV settings.

It was originally built and tested on a Sony Bravia Android TV where reconnecting a Bluetooth headset normally requires opening Settings, navigating to Remotes & Accessories, selecting the paired headset, and pressing Connect. With Bluetooth Quick Connect, a mapped remote color button can trigger the reconnect flow while you stay in Netflix, YouTube, Disney+, Plex, Kodi, live TV, or whatever you are watching.

Search-friendly summary: Sony Bravia Bluetooth headset reconnect, Android TV Bluetooth headphones quick connect, Google TV Bluetooth accessories shortcut, automatic Bluetooth reconnect for paired headphones, Android TV remote color button Bluetooth connector.

## Features

- Quick connect paired Bluetooth headphones or headsets from the Android TV remote.
- Map red, green, yellow, and blue remote buttons independently.
- Try silent A2DP reconnect first, so no settings screen opens when the TV allows it.
- Fall back to Bluetooth settings if silent reconnect is blocked or unavailable.
- Sony Bravia fallback can open the paired-device accessory screen when present.
- Works with already paired Bluetooth audio devices; it does not handle initial pairing.
- Stores mappings locally on the TV.
- Uses a small native Android app with no network access or cloud service.

## Why This Exists

Many Android TV and Sony Bravia TVs do not automatically reconnect Bluetooth headphones reliably. Even when the headset is already paired, reconnecting can require several remote clicks through settings and accessories menus.

Bluetooth Quick Connect is designed for the common living-room use case:

- You are already watching something.
- You turn on your Bluetooth headphones.
- You want one remote button to reconnect audio.
- You do not want to leave playback and browse TV settings.

## Compatibility

Silent reconnect uses Android's hidden `BluetoothA2dp.connect(BluetoothDevice)` API through reflection. This is not part of the public Android SDK, so behavior depends on the TV firmware.

Known behavior:

- Sony Bravia Android TV 9: silent Bluetooth headset reconnect works on the tested device.
- Other Android TV or Google TV devices: may work, may return `false`, may throw, or may be blocked by hidden API restrictions.
- Unsupported devices should not crash; the app catches hidden API failures and tries settings fallbacks.

The app requires enabling its accessibility service because normal Android TV apps cannot globally capture remote color buttons while another app is playing video. See [Why Accessibility Is Required](#why-accessibility-is-required) for details.

## Why Accessibility Is Required

Bluetooth Quick Connect uses Android's accessibility service mechanism only to receive remote-control color button key events while other apps are in the foreground.

Android TV does not provide a normal public API for an app to say "run this action when the red, green, yellow, or blue remote button is pressed globally." Without an accessibility service, the app can only react to buttons while its own setup screen is open.

What the accessibility service does:

- Requests key-event filtering with `flagRequestFilterKeyEvents`.
- Watches for `KEYCODE_PROG_RED`, `KEYCODE_PROG_GREEN`, `KEYCODE_PROG_YELLOW`, and `KEYCODE_PROG_BLUE`.
- Checks whether the pressed color button has a saved Bluetooth device mapping.
- Starts the Bluetooth quick-connect flow for that mapped device.

What the accessibility service does not do:

- It does not read screen text.
- It does not inspect app contents.
- It does not collect accessibility events.
- It does not send data anywhere.
- It does not need internet access.
- It does not consume unrelated keys such as select, back, volume, play/pause, or the help button.

If a color button is not mapped, the service returns `false` so Android TV and other apps can handle the key normally.

## Sony Bravia Notes

On the tested Sony Bravia Android TV, Bluetooth Quick Connect can silently reconnect a paired A2DP headset. If silent reconnect cannot be started, the app tries Sony's paired Bluetooth accessory management screen before falling back to generic Android settings.

Sony Bravia fallback details:

- package: `com.android.tv.settings`
- activity: `com.sony.dtv.settings.accessories.BluetoothAccessoryActivity`
- extras: `accessory_address`, `accessory_name`, `accessory_icon_res`

This Sony-specific fallback is optional and guarded by `resolveActivity()`, so non-Sony Android TV devices should skip it safely.

## Setup

1. Pair your Bluetooth headphones, headset, earbuds, or speaker in Android TV settings first.
2. Install Bluetooth Quick Connect.
3. Enable the accessibility service:

```text
Settings -> Device Preferences -> Accessibility -> Bluetooth Quick Connect
```

4. Open the app from the Android TV launcher.
5. Choose which paired Bluetooth device each remote color button should connect.
6. Press the mapped color button while watching TV to reconnect.

## Fallback Settings Screens

The fallback launcher currently tries these intents in order:

1. Sony Bravia paired-device screen, if present.
2. Generic Android Bluetooth settings: `Settings.ACTION_BLUETOOTH_SETTINGS`.
3. Generic Android settings: `Settings.ACTION_SETTINGS`.

Vendor-specific paired-device screens are not standardized. If another Android TV vendor exposes a device-specific Bluetooth accessory screen, add it in `DeviceSettingsFallback` and keep it guarded by `resolveActivity()` before launching.

Useful ADB commands for discovering vendor Bluetooth or accessories screens on Android TV:

```sh
adb shell cmd package resolve-activity android.settings.BLUETOOTH_SETTINGS
adb shell cmd package query-activities -a android.settings.BLUETOOTH_SETTINGS
adb shell dumpsys package com.android.tv.settings
adb shell dumpsys activity activities
```

For vendor activities that require extras, launch them from ADB first and verify they do not crash before adding them to the app.

## Building With Gradle

The recommended build path is Gradle:

```sh
./gradlew assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Building With The Manual Script

The repository also includes a small manual Android SDK build script. This is useful for debugging the raw Android build steps or building without Gradle.

Install the Android SDK with platform `android-33` and build-tools `35.0.0`, then run:

```sh
./build.sh
```

The script uses `ANDROID_SDK_ROOT` or `ANDROID_HOME`. If neither is set, it checks common SDK locations under your home directory.

Optional overrides:

```sh
ANDROID_BUILD_TOOLS=/path/to/build-tools/35.0.0 ./build.sh
ANDROID_PLATFORM_JAR=/path/to/android-33/android.jar ./build.sh
```

Output:

```text
build/bluetooth-quick-connect.apk
```

## Installing

```sh
adb install -r build/bluetooth-quick-connect.apk
```

Package name:

```text
dk.fez.bluetoothquickconnect
```

## Versioning

Current version: `0.1.0`

Android package version:

```text
versionCode: 1
versionName: 0.1.0
```

See `CHANGELOG.md` for release notes.

## Limitations

- Only already paired Bluetooth devices are supported.
- Silent reconnect is best-effort because it uses hidden Android APIs.
- Some Android TV or Google TV firmware may block silent A2DP reconnect.
- Generic fallback settings cannot always open the exact paired-device detail page.
- Remote color-button key codes must be delivered by the TV/remote and not consumed by another app first.
- Initial Bluetooth pairing still happens in the TV's normal Bluetooth accessories settings.

## Related Search Terms

People looking for this app may search for:

- Sony Bravia Bluetooth headphones reconnect
- Sony Android TV Bluetooth headset quick connect
- Android TV Bluetooth headphones shortcut
- Google TV Bluetooth accessories reconnect
- Bluetooth headset automatic reconnect Android TV
- reconnect paired Bluetooth headphones on TV
- Android TV remote color button shortcut
- Sony Bravia Remotes & Accessories Bluetooth connect

## License

MIT. See `LICENSE`.
