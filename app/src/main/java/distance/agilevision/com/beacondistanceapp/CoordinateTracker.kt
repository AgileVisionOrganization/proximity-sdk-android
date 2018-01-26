package distance.agilevision.com.beacondistanceapp

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface CoordinateTracker {
    fun onCoordinateChange(x: Double, y: Double)
    fun onDistanceChange(i: Identifier, current: Double, medium: Double?)
}