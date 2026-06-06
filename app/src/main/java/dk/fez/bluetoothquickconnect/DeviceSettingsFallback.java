package dk.fez.bluetoothquickconnect;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

/**
 * Opens the best available Bluetooth settings screen when silent reconnect cannot be started.
 */
final class DeviceSettingsFallback {
    private static final String TAG = "BtQuickConnect";

    private static final String SONY_SETTINGS_PACKAGE = "com.android.tv.settings";
    private static final String SONY_ACCESSORY_ACTIVITY =
            "com.sony.dtv.settings.accessories.BluetoothAccessoryActivity";
    private static final String EXTRA_ACCESSORY_ADDRESS = "accessory_address";
    private static final String EXTRA_ACCESSORY_NAME = "accessory_name";
    private static final String EXTRA_ACCESSORY_ICON_RES = "accessory_icon_res";

    private DeviceSettingsFallback() {
    }

    static boolean open(Context context, MappingStore.DeviceMapping mapping) {
        Context appContext = context.getApplicationContext();
        Intent[] candidates = new Intent[] {
                // Vendor-specific paired-device screens are not standardized. Keep them optional
                // and always pass through resolveActivity() before launch so other TVs do not crash.
                createSonyAccessoryIntent(mapping),
                createSettingsIntent(Settings.ACTION_BLUETOOTH_SETTINGS),
                createSettingsIntent(Settings.ACTION_SETTINGS)
        };

        for (Intent candidate : candidates) {
            if (candidate != null && tryStart(appContext, candidate)) {
                return true;
            }
        }

        Log.w(TAG, "No Bluetooth settings fallback activity is available");
        return false;
    }

    private static Intent createSonyAccessoryIntent(MappingStore.DeviceMapping mapping) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SONY_SETTINGS_PACKAGE, SONY_ACCESSORY_ACTIVITY));
        intent.putExtra(EXTRA_ACCESSORY_ADDRESS, mapping.address);
        intent.putExtra(EXTRA_ACCESSORY_NAME, mapping.name);
        intent.putExtra(EXTRA_ACCESSORY_ICON_RES, 0);
        return intent;
    }

    private static Intent createSettingsIntent(String action) {
        return new Intent(action);
    }

    private static boolean tryStart(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) == null) {
            return false;
        }

        try {
            context.startActivity(intent);
            return true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Fallback activity failed: " + intent, exception);
            return false;
        }
    }
}
