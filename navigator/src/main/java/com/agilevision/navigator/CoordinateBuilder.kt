package com.agilevision.navigator


/**
 * Builder class for[CoordinateCalculator]
 * */
class CoordinateBuilder {

    class InvalidConfigException(override var message: String) : Exception(message)

    val SIGNAL_LOSS_1M = 41;

    /**@see [withCalcMethod]*/
    val COEF_1 = 0.89976
    /**@see [withCalcMethod]*/
    val COEF_2 = 7.7095
    /**@see [withCalcMethod]*/
    val COEF_3 = 0.111

    private fun calculateDistance(rssi: Double, txPower: Int): Double {
        return Math.pow(10.0, (txPower.toDouble() - SIGNAL_LOSS_1M - rssi) / (10 * 2))
    }

    private fun calcDistanceConstants(p1: Double, p2: Double, p3: Double): (Double, Int) -> Double {
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
    var cacheTime: Int? = 4000
        private set
    var logs: Boolean = false
        private set
    var calcMethod: (Double, Int) -> Double = ::calculateDistance
        private set
    var distanceTracker: DistanceTracker? = null
        private set


    /**
     * Registers beacons with specified coordinates
     * @param beacons - updates registered beacon with these ones
     * */
    fun addBeacons(beacons: Map<Beacon, XYPoint>) = apply { beaconsCorners.putAll(beacons) }

    /**
     * Registers beacon with specifies coordinates.
     * @param beacon unique identifier for beacon
     * @param point coordinate of beacon
     * */
    fun addBeacon(beacon: Beacon, point: XYPoint) = apply {
        if (beaconsCorners.containsKey(beacon)) {
            throw InvalidConfigException("Beacon $beacon already exists")
        }
        beaconsCorners.put(beacon, point)
    }

    /**
     * Registers beacon with specifies coordinates.
     * @param namespace part of unique identifier for beacon
     * @param instance part of unique identifier for beacon
     * @param x abscissa of the specified beacon
     * @param y ordinate the specified beacon
     * */
    fun addBeacon(namespace: String, instance: String, x: Double, y: Double) = apply {
        addBeacon(Beacon(namespace, instance), XYPoint(x, y))
    }

    fun trackCoordinate(tracker: CoordinateTracker) = apply { this.tracker = tracker }

    /**
     * Prints information when new devices is found and more info about calculated coordinates
     * */
    fun enableLogs() = apply { this.logs = true }

    /**
     * Registers callback that will be called each BLE finds beacon added with [addBeacon]
     * */
    fun trackDistance(distanceTracker: DistanceTracker) = apply { this.distanceTracker = distanceTracker }

    /**
     * By default all beacons are put to cache, to be specific theirs Rssi and TxPower
     * When time comes to calculate distance, We get the average Rssi for beacon and calculate distance for it
     * @param cacheTime determines how long beacons are saved in cache. With increasing this time accuracy of coordinate will be increased but it will take more time for coordinate to update
     * */
    fun cacheTime(cacheTime: Int?) = apply { this.cacheTime = cacheTime }

    /**
     * Replaces default method for distance calculation with foruma below:
     * The formula is (coef1)*((rssi/power)^ coef2) + coef3
     * */
    fun withCalcMethod(coef1: Double, coef2: Double, coef3: Double) = apply {
        calcMethod = calcDistanceConstants(coef1, coef2, coef3)
    }

    /**
     * Creates [CoordinateCalculator]
     * @throws InvalidConfigException if configuration is invalid
     * */
    fun build(): CoordinateCalculator {
        if (beaconsCorners.size < 3) {
            throw InvalidConfigException("Configuration should contain at least 3 different beacons")
        }
        if (tracker == null) {
            throw InvalidConfigException("You should specify coordTracker callback")
        }
        return CoordinateCalculator(beaconsCorners, tracker!!, cacheTime, distanceTracker, calcMethod, logs);
    }
}