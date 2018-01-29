package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface BeaconsTracker {
    fun onBeaconDistanceFound(beacon: Beacon, rssi: Int, txPower: Int)
}