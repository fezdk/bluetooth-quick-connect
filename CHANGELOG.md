# Changelog

All notable changes to Bluetooth Quick Connect are documented here.

## 0.1.0 - 2026-06-06

- Initial public source release.
- Added Android TV setup screen for mapping red, green, yellow, and blue remote buttons to paired Bluetooth devices.
- Added global color-button handling through an accessibility service.
- Added best-effort silent A2DP reconnect using Android's hidden `BluetoothA2dp.connect(BluetoothDevice)` API.
- Added safe fallback to Sony Bravia paired-device settings, generic Bluetooth settings, and generic Android settings.
- Added Sony Bravia Android TV 9 compatibility notes.
- Added TV launcher banner and ghost-with-headphones app icon.
- Added Gradle project and Gradle wrapper for standard Android builds.
- Added portable manual Android SDK build script.
