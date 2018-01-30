package io.agilevision.priximity.indoor

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface CoordinateTracker {


    /**
     * This method is called when your position of current device changes. This position is caclulated from beacon you registered.
     * @param i - identifier of the beacon
     * @param x - abscissa of current device
     * @param y - orrdinate of current device
     * */
    fun onCoordinateChange(x: Double, y: Double)
}