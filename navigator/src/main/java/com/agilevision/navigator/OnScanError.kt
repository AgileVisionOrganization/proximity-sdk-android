package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface OnScanError {


    enum class ErrorType(var code: Int, var description: String) {
        ALREADY_STARTED(0, "Scan is already running"),
        APPLICATION_REGISTRATION_FAILED(1, "Bluetooth integration error"),
        FEATURE_UNSUPPORTED(2, "Scan is unsupported on this device"),
        INTERNAL_ERROR(3, "Internal Bluetooth error"),
        NO_BLUETOOTH(4, "No ble"),
        NOT_ENABLED(5, "NO bt enabled"),
        BT_DISABLED(6, "Bt enabled, try again"),
        UNKNOWN(7, "Unknown error"),
    }


    fun onError(description: ErrorType)
}