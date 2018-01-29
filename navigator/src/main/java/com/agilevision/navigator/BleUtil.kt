package com.agilevision.navigator

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import android.util.SparseIntArray
import java.util.*

/**
 * @author Andrew Koidan, AgileVision, 15.12.17.
 */
class BleUtil(private val callback: OnScanError, private val onBeaconFound: OnScanResult) : ScanCallback() {
    protected var scanRunning = false


    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val errorDescription = SparseIntArray()
    private var btAdapter: BluetoothAdapter? = null
    private var scanSettings: ScanSettings? = null
    private var scanFilters: List<ScanFilter>? = null

    init {
        errorDescription.append(ScanCallback.SCAN_FAILED_ALREADY_STARTED, R.string.scan_in_progress)
        errorDescription.append(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, R.string.ble_failed_app_reg)
        errorDescription.append(ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED, R.string.ble_scan_unsupp)
        errorDescription.append(ScanCallback.SCAN_FAILED_INTERNAL_ERROR, R.string.ble_internal_error)
    }

    private fun getScanner(a: Activity): BluetoothLeScanner? {
        if (this.bluetoothLeScanner == null) {
            val btManager = a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (btManager == null) {
                callback.onError("No ble")
            } else {
                btAdapter = btManager.adapter
                if (btAdapter == null || !btAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    a.startActivityForResult(enableBtIntent, SCAN_CODE)
                    callback.onError("NO bt enabled")
                    return null
                } else {
                    bluetoothLeScanner = btAdapter!!.bluetoothLeScanner
                    return bluetoothLeScanner
                }
            }
        } else if (!btAdapter!!.isEnabled) {
            val enable = btAdapter!!.enable()
            callback.onError("Bt enabled, try again")
            return null
        }
        return bluetoothLeScanner
    }

    fun scanForDevices(a: Activity) {
        if (this.getScanner(a) != null && !scanRunning) {
            scanRunning = true
            this.bluetoothLeScanner!!.startScan(getScanFilters(), getScanSettings(), this)
        } else {
            callback.onError("Scan is already in progress")
        }
    }


    private fun getScanFilters(): List<ScanFilter> {
        if (scanFilters == null) {
            scanFilters = Arrays.asList(ScanFilter.Builder().setServiceUuid(EDDYSTONE_SERVICE_UUID).build())
        }
        return scanFilters as List<ScanFilter>
    }


    private fun getScanSettings(): ScanSettings {
        if (scanSettings == null) {
            scanSettings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build()
        }
        return scanSettings as ScanSettings
    }

    fun Byte.toPositiveInt() = toInt() and 0xFF


    fun toHexString(bytes: ByteArray): String {
        val chars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val c = bytes[i].toPositiveInt()
            chars[i * 2] = HEX[c.ushr(4)]
            chars[i * 2 + 1] = HEX[c and 0x0F]
        }
        return String(chars).toLowerCase()
    }


    override fun onScanResult(callbackType: Int, result: ScanResult) {
        val scanRecord = result.scanRecord
        if (scanRecord != null) {
            val serviceData = scanRecord.getServiceData(BleUtil.EDDYSTONE_SERVICE_UUID)
            if (serviceData != null) {
                // We're only interested in the UID frame time since we need the beacon ID to register.
                if (serviceData[0] == BleUtil.EDDYSTONE_UID_FRAME_TYPE) {
                    // Extract the beacon ID from the service data. Offset 0 is the frame type, 1 is the
                    // Tx power, and the next 16 are the ID.
                    // See https://github.com/google/eddystone/tree/master/eddystone-uid for more information.
                    val nameSpace = toHexString(Arrays.copyOfRange(serviceData, NID_START, NID_END))
                    val instance = toHexString(Arrays.copyOfRange(serviceData, BID_START, BID_END))
                    val txPowerLevel = serviceData[TX_POWER_OFFSET].toInt()

                    // Log.d("BLe", "Found " + nameSpace + ": " + instance + ";" + result.getDevice().getAddress() + ";tx=" + txPowerLevel  +";rrsi"+ result.getRssi());
                    onBeaconFound.onBeaconDistanceFound(Identifier(nameSpace, instance), result.rssi, txPowerLevel)

                }
            }
        }
    }

    fun stopScan() {
        if (bluetoothLeScanner != null && btAdapter != null && btAdapter!!.isEnabled) {
            bluetoothLeScanner!!.stopScan(this)
        }
        scanRunning = false
    }

    override fun onScanFailed(errorCode: Int) {
        stopScan()
        val errorDescription = this.errorDescription.get(errorCode)
        callback.onError(errorDescription.toString())
    }

    companion object {

        private val SCAN_CODE = 111
        private val EDDYSTONE_SERVICE_UUID = ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB")
        private val EDDYSTONE_UID_FRAME_TYPE: Byte = 0x00
        private val HEX = "0123456789ABCDEF".toCharArray()
        /**
         * https://github.com/google/eddystone/tree/master/eddystone-uid#frame-specification
         */
        private val TX_POWER_OFFSET = 1
        private val NID_START = 2
        private val NID_END = 12
        private val BID_START = NID_END
        private val BID_END = 18
    }


}
