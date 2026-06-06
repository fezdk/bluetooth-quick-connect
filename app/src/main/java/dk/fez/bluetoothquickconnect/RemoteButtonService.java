package dk.fez.bluetoothquickconnect;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service that maps color remote-control keys to paired Bluetooth settings screens.
 *
 * <p>Android does not let normal sideloaded apps bind global hardware keys directly. When this
 * service is enabled by the user or via ADB, Android forwards key events here and the service can
 * consume the selected key.</p>
 */
public final class RemoteButtonService extends AccessibilityService {
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
            setServiceInfo(info);
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_UP || !isColorButton(event.getKeyCode())) {
            return false;
        }

        MappingStore.DeviceMapping mapping = MappingStore.get(this, event.getKeyCode());
        if (mapping == null) {
            return false;
        }

        BluetoothConnectAction.connectOrOpen(this, mapping);
        return true;
    }

    private static boolean isColorButton(int keyCode) {
        for (MappingStore.ColorButton button : MappingStore.COLOR_BUTTONS) {
            if (button.keyCode == keyCode) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
