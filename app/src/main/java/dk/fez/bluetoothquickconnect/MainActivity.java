package dk.fez.bluetoothquickconnect;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Android TV settings screen for assigning color remote buttons to paired Bluetooth devices.
 *
 * <p>The accessibility service reads the mappings saved here, tries a silent A2DP reconnect, and
 * falls back to the best available Bluetooth settings screen when reconnect cannot be started.</p>
 */
public final class MainActivity extends Activity {
    private final List<DeviceOption> deviceOptions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(17, 24, 39)));
        loadPairedDevices();
        setContentView(createContentView());
    }

    private View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.rgb(17, 24, 39));
        scrollView.setFillViewport(false);
        scrollView.setFadingEdgeLength(0);
        scrollView.setClipToPadding(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(17, 24, 39));
        root.setPadding(48, 40, 48, 40);
        scrollView.addView(root);

        TextView title = new TextView(this);
        title.setText("Bluetooth Quick Connect");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.START);
        root.addView(title);

        TextView description = new TextView(this);
        description.setText("Choose which already paired Bluetooth device each color button connects. Select Disabled to leave a color button untouched.");
        description.setTextSize(18);
        description.setTextColor(Color.rgb(209, 213, 219));
        description.setPadding(0, 12, 0, 24);
        root.addView(description);

        for (MappingStore.ColorButton button : MappingStore.COLOR_BUTTONS) {
            addMappingRow(root, button);
        }

        Button refreshButton = new Button(this);
        refreshButton.setText("Refresh paired devices");
        styleActionButton(refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadPairedDevices();
                setContentView(createContentView());
            }
        });
        root.addView(refreshButton);

        return scrollView;
    }

    private void addMappingRow(LinearLayout root, final MappingStore.ColorButton button) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.row_panel);
        row.setPadding(24, 20, 24, 24);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, 22);
        root.addView(row);
        row.setLayoutParams(rowParams);

        TextView label = new TextView(this);
        label.setText(button.label + " button");
        label.setTextSize(22);
        label.setTextColor(Color.WHITE);
        row.addView(label);

        final Button mappingButton = new Button(this);
        styleActionButton(mappingButton);
        updateMappingButtonText(button.keyCode, mappingButton);
        row.addView(mappingButton);

        final Button testButton = new Button(this);
        testButton.setText("Connect selected");
        styleActionButton(testButton);
        updateConnectButtonState(button.keyCode, testButton);
        mappingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDeviceSelection(button, mappingButton, testButton);
            }
        });
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MappingStore.DeviceMapping mapping = MappingStore.get(MainActivity.this, button.keyCode);
                if (mapping == null) {
                    return;
                }
                BluetoothConnectAction.connectOrOpen(MainActivity.this, mapping);
            }
        });
        row.addView(testButton);
    }

    private void showDeviceSelection(
            final MappingStore.ColorButton button,
            final Button mappingButton,
            final Button testButton) {
        String[] labels = new String[deviceOptions.size()];
        for (int i = 0; i < deviceOptions.size(); i++) {
            labels[i] = deviceOptions.get(i).toString();
        }

        new AlertDialog.Builder(this)
                .setTitle(button.label + " button device")
                .setSingleChoiceItems(labels, findSelectedIndex(button.keyCode), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeviceOption option = deviceOptions.get(which);
                        MappingStore.save(MainActivity.this, button.keyCode, option.toMapping());
                        updateMappingButtonText(button.keyCode, mappingButton);
                        updateConnectButtonState(button.keyCode, testButton);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMappingButtonText(int keyCode, Button button) {
        MappingStore.DeviceMapping mapping = MappingStore.get(this, keyCode);
        if (mapping == null) {
            button.setText("Choose device: Disabled");
        } else {
            button.setText("Choose device: " + mapping.name);
        }
    }

    private void updateConnectButtonState(int keyCode, Button button) {
        boolean hasMapping = MappingStore.get(this, keyCode) != null;
        button.setEnabled(hasMapping);
        button.setAlpha(hasMapping ? 1.0f : 0.45f);
    }

    private void styleActionButton(Button button) {
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(24, 8, 24, 8);
        button.setMinHeight(56);
        button.setBackgroundResource(R.drawable.button_selector);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 12, 0, 0);
        button.setLayoutParams(params);
    }

    private int findSelectedIndex(int keyCode) {
        MappingStore.DeviceMapping mapping = MappingStore.get(this, keyCode);
        if (mapping == null) {
            return 0;
        }

        for (int i = 0; i < deviceOptions.size(); i++) {
            DeviceOption option = deviceOptions.get(i);
            if (option.address != null && option.address.equals(mapping.address)) {
                return i;
            }
        }
        return 0;
    }

    private void loadPairedDevices() {
        deviceOptions.clear();
        deviceOptions.add(DeviceOption.disabled());

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
            if (bondedDevices != null) {
                List<DeviceOption> paired = new ArrayList<>();
                for (BluetoothDevice device : bondedDevices) {
                    String address = device.getAddress();
                    if (address == null || address.length() == 0) {
                        continue;
                    }
                    String name = device.getName();
                    if (name == null || name.length() == 0) {
                        name = address;
                    }
                    paired.add(new DeviceOption(name, address));
                }

                Collections.sort(paired, new Comparator<DeviceOption>() {
                    @Override
                    public int compare(DeviceOption left, DeviceOption right) {
                        return left.label.compareToIgnoreCase(right.label);
                    }
                });

                deviceOptions.addAll(paired);
            }
        }

        addSavedMappingsIfMissing();
    }

    private void addSavedMappingsIfMissing() {
        for (MappingStore.ColorButton button : MappingStore.COLOR_BUTTONS) {
            MappingStore.DeviceMapping mapping = MappingStore.get(this, button.keyCode);
            if (mapping != null && !hasDeviceOption(mapping.address)) {
                deviceOptions.add(new DeviceOption(mapping.name, mapping.address));
            }
        }
    }

    private boolean hasDeviceOption(String address) {
        for (DeviceOption option : deviceOptions) {
            if (option.address != null && option.address.equals(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Display item for the device mapping selector.
     */
    private static final class DeviceOption {
        final String label;
        final String address;

        DeviceOption(String label, String address) {
            this.label = label;
            this.address = address;
        }

        static DeviceOption disabled() {
            return new DeviceOption("Disabled", null);
        }

        MappingStore.DeviceMapping toMapping() {
            if (address == null) {
                return null;
            }
            return new MappingStore.DeviceMapping(address, label);
        }

        @Override
        public String toString() {
            if (address == null) {
                return label;
            }
            return label + " (" + address + ")";
        }
    }
}
