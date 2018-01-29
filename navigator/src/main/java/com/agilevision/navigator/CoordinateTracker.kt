package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface CoordinateTracker {
    fun onCoordinateChange(x: Double, y: Double)
}