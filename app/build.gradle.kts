plugins {
    id("com.android.application")
}

android {
    namespace = "dk.fez.bluetoothquickconnect"
    compileSdk = 33

    defaultConfig {
        applicationId = "dk.fez.bluetoothquickconnect"
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "0.1.0"
    }
}
