package dk.fez.bluetoothquickconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;

/**
 * Persists color-button to paired-Bluetooth-device mappings for the accessibility service.
 */
final class MappingStore {
    private static final String PREFS_NAME = "quick_connect_mappings";
    private static final String KEY_ADDRESS_PREFIX = "address_";
    private static final String KEY_NAME_PREFIX = "name_";

    static final ColorButton[] COLOR_BUTTONS = new ColorButton[] {
            new ColorButton("Red", KeyEvent.KEYCODE_PROG_RED),
            new ColorButton("Green", KeyEvent.KEYCODE_PROG_GREEN),
            new ColorButton("Yellow", KeyEvent.KEYCODE_PROG_YELLOW),
            new ColorButton("Blue", KeyEvent.KEYCODE_PROG_BLUE)
    };

    private MappingStore() {
    }

    static DeviceMapping get(Context context, int keyCode) {
        SharedPreferences prefs = prefs(context);
        String address = prefs.getString(addressKey(keyCode), null);
        String name = prefs.getString(nameKey(keyCode), null);
        if (address == null || address.length() == 0) {
            return null;
        }
        return new DeviceMapping(address, name != null ? name : address);
    }

    static void save(Context context, int keyCode, DeviceMapping mapping) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (mapping == null) {
            editor.remove(addressKey(keyCode));
            editor.remove(nameKey(keyCode));
        } else {
            editor.putString(addressKey(keyCode), mapping.address);
            editor.putString(nameKey(keyCode), mapping.name);
        }
        editor.apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String addressKey(int keyCode) {
        return KEY_ADDRESS_PREFIX + keyCode;
    }

    private static String nameKey(int keyCode) {
        return KEY_NAME_PREFIX + keyCode;
    }

    /**
     * Color remote button descriptor.
     */
    static final class ColorButton {
        final String label;
        final int keyCode;

        ColorButton(String label, int keyCode) {
            this.label = label;
            this.keyCode = keyCode;
        }
    }

    /**
     * Saved Bluetooth device target for a color button.
     */
    static final class DeviceMapping {
        final String address;
        final String name;

        DeviceMapping(String address, String name) {
            this.address = address;
            this.name = name;
        }
    }
}
