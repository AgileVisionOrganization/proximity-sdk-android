package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 29.01.18.
 */
interface DistanceTracker {

    /**
     * This method is called when distance to beacon is changed
     *
     * @param i - identifier of the beacon
     * @param current - current distance to the beacon at this moment of time
     * @param medium - calculated average distance to beacon for [com.agilevision.navigator.CoordinateBuilder.cacheTime]
     * */
    fun onDistanceChange(i: Beacon, current: Double, medium: Double)
}