package com.agilevision.navigator

/**
 * @author Andrew Koidan, AgileVision, 29.01.18.
 */
class CoordinateBuilder {

    class InvalidConfigException(override var message:String): Exception(message)

    val SIGNAL_LOSS_1M = 41;
    val COEF_1 = 0.89976
    val COEF_2 = 7.7095
    val COEF_3 = 0.111

    fun calculateDistance(rssi: Double, txPower: Int): Double {
        return Math.pow(10.0, ( txPower.toDouble() - SIGNAL_LOSS_1M - rssi) / (10 * 2))
    }

    fun calcDistanceConstants(p1: Double, p2: Double, p3: Double): (Double, Int) -> Double {
        return fun(rssi: Double, txPower: Int): Double {
            val ratio = rssi * 1.0 / (txPower - SIGNAL_LOSS_1M);
            if (ratio < 1.0) {
                return Math.pow(ratio, 10.0);
            } else {
                return (p1) * Math.pow(ratio, p2) + p3;
            }
        }
    }

    var beaconsCorners: MutableMap<Beacon, XYPoint> = mutableMapOf()
        private set
    var tracker: CoordinateTracker? = null
        private set
    var cacheTime: Int = 4000
        private set
    var logs: Boolean = false
        private set
    var calcMethod: (Double, Int) -> Double = ::calculateDistance
        private set
    var distanceTracker: DistanceTracker? = null
        private set

    fun addBeacons(beacons: Map<Beacon, XYPoint>) = apply { beaconsCorners.putAll(beacons) }
    fun addBeacon(beacon: Beacon, point: XYPoint) = apply {
        beaconsCorners.put(beacon, point)
    }
    fun addBeacon(namespace: String, instance: String, x: Double, y: Double) = apply {
        beaconsCorners.put(Beacon(namespace, instance), XYPoint(x,y))
    }
    fun trackCoordinate(tracker: CoordinateTracker) = apply { this.tracker = tracker }
    fun enableLogs() = apply { this.logs = true }
    fun trackDistance(distanceTracker: DistanceTracker) = apply { this.distanceTracker = distanceTracker }
    fun cacheTime(cacheTime: Int) = apply { this.cacheTime = cacheTime }
    fun withCalcMethod(coef1: Double, coef2: Double, coef3: Double) = apply {
        calcMethod = calcDistanceConstants(coef1, coef2, coef3)
    }

    fun build(): CoordinateCalculator  {
        if (beaconsCorners.size < 3) {
            throw InvalidConfigException("Configuration should contain at least 3 beacons")
        }
        if (tracker == null) {
            throw InvalidConfigException("You should specify coordTracker callback")
        }
        return CoordinateCalculator(beaconsCorners, tracker!!, cacheTime, distanceTracker, calcMethod, logs);
    }
}