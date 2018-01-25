package distance.agilevision.com.beacondistanceapp

/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
interface OnScan {
    fun startScan()
    fun onError(description: String)
}