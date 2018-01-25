package distance.agilevision.com.beacondistanceapp

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface OnScanResult {
    fun onBeaconDistanceFound(beacon: Identifier, distance: Double)
}