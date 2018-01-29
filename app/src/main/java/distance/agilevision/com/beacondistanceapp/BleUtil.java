package distance.agilevision.com.beacondistanceapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseIntArray;


import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Andrew Koidan, AgileVision, 15.12.17.
 */
public class BleUtil extends ScanCallback  {

    public static final int MAX_DISTANCE_PROGRESS = 100;
    private static final int SCAN_TIME = 4000;
    private static final int SCAN_CODE = 111;
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final byte EDDYSTONE_UID_FRAME_TYPE = 0x00;
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    /**
     * https://github.com/google/eddystone/tree/master/eddystone-uid#frame-specification
     */
    private static final int TX_POWER_OFFSET = 1;
    private static final int NID_START = 2;
    private static final int NID_END = 12;
    private static final int BID_START = NID_END;
    private static final int BID_END = 18;
    private final OnScan callback;
    private final OnScanResult onBeaconFound;
    protected boolean scanRunning = false;


    protected Set<BleBeacon> btDevices = new HashSet<>();
    private BluetoothLeScanner bluetoothLeScanner;
    private SparseIntArray errorDescription = new SparseIntArray();
    private BluetoothAdapter btAdapter;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters;

    public BleUtil(OnScan beaconDataCb, OnScanResult r) {
        this.callback = beaconDataCb;
        this.onBeaconFound = r;
        errorDescription.append(SCAN_FAILED_ALREADY_STARTED, R.string.scan_in_progress);
        errorDescription.append(SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, R.string.ble_failed_app_reg);
        errorDescription.append(SCAN_FAILED_FEATURE_UNSUPPORTED, R.string.ble_scan_unsupp);
        errorDescription.append(SCAN_FAILED_INTERNAL_ERROR, R.string.ble_internal_error);
    }

    private BluetoothLeScanner getScanner(Activity a) {
        if (this.bluetoothLeScanner == null) {
            BluetoothManager btManager =
                    (BluetoothManager) a.getSystemService(Context.BLUETOOTH_SERVICE);
            if (btManager == null) {
                callback.onError("No ble");
            } else {
                btAdapter = btManager.getAdapter();
                if (btAdapter == null || !btAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    a.startActivityForResult(enableBtIntent, SCAN_CODE);
                    callback.onError("NO bt enabled");
                    return null;
                } else {
                    return bluetoothLeScanner = btAdapter.getBluetoothLeScanner();
                }
            }
        } else if (!btAdapter.isEnabled()) {
            boolean enable = btAdapter.enable();
            callback.onError("Bt enabled, try again");
            return null;
        }
        return bluetoothLeScanner;
    }

    public void scanForDevices(Activity a) {
        if (!PermissionUtils.isBluetoothGranted(a)) {
            PermissionUtils.askBTPermissions(a);
        } else if (this.getScanner(a) != null && !scanRunning) {
            btDevices.clear();
            scanRunning = true;
            callback.startScan();
            Log.d("BLE", "Starting scan");
            this.bluetoothLeScanner.startScan(getScanFilters(), getScanSettings(), this);
        }
    }


    private List<ScanFilter> getScanFilters() {
        if (scanFilters == null) {
            scanFilters = Arrays.asList(new ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build());
        }
        return scanFilters;
    }


    private ScanSettings getScanSettings() {
        if (scanSettings == null) {
            scanSettings = new ScanSettings.Builder().
                    setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();
        }
        return scanSettings;
    }

    public String toHexString(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xFF;
            chars[i * 2] = HEX[c >>> 4];
            chars[i * 2 + 1] = HEX[c & 0x0F];
        }
        return new String(chars).toLowerCase();
    }


    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        ScanRecord scanRecord = result.getScanRecord();
        if (scanRecord != null) {
            byte[] serviceData = scanRecord.getServiceData(BleUtil.EDDYSTONE_SERVICE_UUID);
            if (serviceData != null) {
                // We're only interested in the UID frame time since we need the beacon ID to register.
                if (serviceData[0] == BleUtil.EDDYSTONE_UID_FRAME_TYPE) {
                    // Extract the beacon ID from the service data. Offset 0 is the frame type, 1 is the
                    // Tx power, and the next 16 are the ID.
                    // See https://github.com/google/eddystone/tree/master/eddystone-uid for more information.
                    String nameSpace = toHexString(Arrays.copyOfRange(serviceData, NID_START, NID_END));
                    String instance = toHexString(Arrays.copyOfRange(serviceData, BID_START, BID_END));
                    int txPowerLevel = serviceData[TX_POWER_OFFSET];

                   // Log.d("BLe", "Found " + nameSpace + ": " + instance + ";" + result.getDevice().getAddress() + ";tx=" + txPowerLevel  +";rrsi"+ result.getRssi());
                    onBeaconFound.onBeaconDistanceFound(new Identifier(nameSpace, instance), result.getRssi(), txPowerLevel);

                }
            }
        }
    }

    private void stopScan() {
        if (bluetoothLeScanner != null && btAdapter != null && btAdapter.isEnabled()) {
            bluetoothLeScanner.stopScan(this);
        }
        scanRunning = false;
    }

    @Override
    public void onScanFailed(int errorCode) {
        stopScan();
        Integer errorDescription = this.errorDescription.get(errorCode);
        callback.onError(String.valueOf(errorDescription));
    }


}
