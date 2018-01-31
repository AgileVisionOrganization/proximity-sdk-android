package io.agilevision.proximity.indoor

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.os.ParcelUuid
import java.util.*

/**
 * @author Andrew Koidan, AgileVision, 15.12.17.
 */
class BeaconsSearcher(private val callback: OnScanError, private val onBeaconFound: BeaconsTracker) : ScanCallback() {
    var scanRunning = false
        private set

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val errorDescription: Map<Int, OnScanError.ErrorType> = mapOf(
            Pair(ScanCallback.SCAN_FAILED_ALREADY_STARTED, OnScanError.ErrorType.ALREADY_STARTED),
            Pair(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED, OnScanError.ErrorType.APPLICATION_REGISTRATION_FAILED),
            Pair(ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED, OnScanError.ErrorType.FEATURE_UNSUPPORTED),
            Pair(ScanCallback.SCAN_FAILED_INTERNAL_ERROR, OnScanError.ErrorType.INTERNAL_ERROR)
    )

    private var btAdapter: BluetoothAdapter? = null
    private var scanSettings: ScanSettings? = null
    private var scanFilters: List<ScanFilter>? = null

    private fun getScanner(a: Activity): BluetoothLeScanner? {
        if (this.bluetoothLeScanner == null) {
            val btManager = a.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            if (btManager == null) {
                callback.onError(OnScanError.ErrorType.NO_BLUETOOTH)
            } else {
                btAdapter = btManager.adapter
                if (btAdapter == null || !btAdapter!!.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    a.startActivityForResult(enableBtIntent, SCAN_CODE)
                    callback.onError(OnScanError.ErrorType.NOT_ENABLED)
                    return null
                } else {
                    bluetoothLeScanner = btAdapter!!.bluetoothLeScanner
                    return bluetoothLeScanner
                }
            }
        } else if (!btAdapter!!.isEnabled) {
            val enable = btAdapter!!.enable()
            callback.onError(OnScanError.ErrorType.BT_DISABLED)
            return null
        }
        return bluetoothLeScanner
    }

    fun scanForDevices(a: Activity) {
        if (this.getScanner(a) != null && !scanRunning) {
            scanRunning = true
            this.bluetoothLeScanner!!.startScan(getScanFilters(), getScanSettings(), this)
        } else {
            callback.onError(OnScanError.ErrorType.ALREADY_STARTED)
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
            val serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID)
            if (serviceData != null) {
                if (serviceData[0] == EDDYSTONE_UID_FRAME_TYPE) {
                    val nameSpace = toHexString(Arrays.copyOfRange(serviceData, NID_START, NID_END))
                    val instance = toHexString(Arrays.copyOfRange(serviceData, BID_START, BID_END))
                    val txPowerLevel = serviceData[TX_POWER_OFFSET].toInt()
                    onBeaconFound.onBeaconDistanceFound(Beacon(nameSpace, instance), result.rssi, txPowerLevel)

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
        if (errorDescription == null) {
            callback.onError(OnScanError.ErrorType.UNKNOWN)
        } else {
            callback.onError(errorDescription)
        }
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
