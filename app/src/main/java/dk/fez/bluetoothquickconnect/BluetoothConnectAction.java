package dk.fez.bluetoothquickconnect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Attempts a silent Bluetooth A2DP reconnect before falling back to Bluetooth settings.
 */
final class BluetoothConnectAction {
    private static final String TAG = "BtQuickConnect";
    private static final long PROFILE_PROXY_TIMEOUT_MS = 2000;

    private BluetoothConnectAction() {
    }

    static void connectOrOpen(Context context, MappingStore.DeviceMapping mapping) {
        if (mapping == null) {
            return;
        }

        Context appContext = context.getApplicationContext();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !isAdapterEnabled(adapter)) {
            DeviceSettingsFallback.open(appContext, mapping);
            return;
        }

        final BluetoothDevice device;
        try {
            device = adapter.getRemoteDevice(mapping.address);
        } catch (IllegalArgumentException exception) {
            DeviceSettingsFallback.open(appContext, mapping);
            return;
        }

        final Handler handler = new Handler(Looper.getMainLooper());
        final AtomicBoolean completed = new AtomicBoolean(false);
        final Runnable timeout = new Runnable() {
            @Override
            public void run() {
                if (completed.compareAndSet(false, true)) {
                    DeviceSettingsFallback.open(appContext, mapping);
                }
            }
        };

        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                handler.removeCallbacks(timeout);
                boolean connectStarted = false;
                try {
                    if (profile == BluetoothProfile.A2DP) {
                        connectStarted = invokeA2dpConnect(proxy, device);
                    }
                } finally {
                    closeProfileProxy(adapter, profile, proxy);
                }

                if (completed.compareAndSet(false, true) && !connectStarted) {
                    DeviceSettingsFallback.open(appContext, mapping);
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.A2DP && completed.compareAndSet(false, true)) {
                    handler.removeCallbacks(timeout);
                    DeviceSettingsFallback.open(appContext, mapping);
                }
            }
        };

        if (!getA2dpProfileProxy(adapter, appContext, listener)) {
            DeviceSettingsFallback.open(appContext, mapping);
            return;
        }

        handler.postDelayed(timeout, PROFILE_PROXY_TIMEOUT_MS);
    }

    private static boolean isAdapterEnabled(BluetoothAdapter adapter) {
        try {
            return adapter.isEnabled();
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to read Bluetooth adapter state", exception);
            return false;
        }
    }

    private static boolean getA2dpProfileProxy(
            BluetoothAdapter adapter,
            Context context,
            BluetoothProfile.ServiceListener listener) {
        try {
            return adapter.getProfileProxy(context, listener, BluetoothProfile.A2DP);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to obtain A2DP profile proxy", exception);
            return false;
        } catch (LinkageError error) {
            Log.w(TAG, "A2DP profile proxy is unavailable on this platform", error);
            return false;
        }
    }

    private static boolean invokeA2dpConnect(BluetoothProfile proxy, BluetoothDevice device) {
        try {
            Method connectMethod = proxy.getClass().getMethod("connect", BluetoothDevice.class);
            Object result = connectMethod.invoke(proxy, device);
            return Boolean.TRUE.equals(result);
        } catch (Exception exception) {
            Log.w(TAG, "Hidden A2DP connect API failed", exception);
            return false;
        } catch (LinkageError error) {
            Log.w(TAG, "Hidden A2DP connect API is unavailable", error);
            return false;
        }
    }

    private static void closeProfileProxy(
            BluetoothAdapter adapter,
            int profile,
            BluetoothProfile proxy) {
        try {
            adapter.closeProfileProxy(profile, proxy);
        } catch (RuntimeException exception) {
            Log.w(TAG, "Unable to close Bluetooth profile proxy", exception);
        } catch (LinkageError error) {
            Log.w(TAG, "Unable to close Bluetooth profile proxy", error);
        }
    }
}
