package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 29.01.18.
 */
interface DistanceTracker {
    fun onDistanceChange(i: Beacon, current: Double, medium: Double)
}