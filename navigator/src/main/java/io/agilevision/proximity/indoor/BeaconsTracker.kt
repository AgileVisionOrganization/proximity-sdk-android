package io.agilevision.proximity.indoor

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface BeaconsTracker {

    /**
     * This method is called when BLE finds any BLE device. It can be absent from your registered beacons.
     * @param beacon - identifier of the beacon
     * @param rssi - Rssi power ( current signal) This value should be more (by module) than -41db
     * @param txPower - The constant firmwared into BLE device which is used to calculate distance to the beacon. This can be reprogrammed with BeaconTools or BeaconsSet. This value is usually between -20..0
     * */
    fun onBeaconDistanceFound(beacon: Beacon, rssi: Int, txPower: Int)
}