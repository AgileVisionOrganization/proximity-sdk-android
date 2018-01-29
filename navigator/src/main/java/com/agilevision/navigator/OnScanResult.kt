package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface OnScanResult {
    fun onBeaconDistanceFound(beacon: Identifier, rssi: Int, txPower: Int)
}