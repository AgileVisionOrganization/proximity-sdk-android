package com.agilevision.navigator

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.util.*


/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
class CoordinateCalculator private constructor(
        var beaconsCorners: Map<Identifier, Point>,
        var coordTracker: CoordinateTracker,
        var cacheTime: Int,
        var distanceTracker: DistanceTracker?,
        var calcMethod: (Double, Int) -> Double) : OnScanResult {

    var x: Double? = null
    var y: Double? = null


    class InvalidConfigException(override var message:String): Exception(message)

    class Builder {

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

        var beaconsCorners: MutableMap<Identifier, Point> = mutableMapOf()
            private set
        var tracker: CoordinateTracker? = null
            private set
        var cacheTime: Int = 4000
            private set
        var calcMethod: (Double, Int) -> Double = ::calculateDistance
            private set
        var distanceTracker: DistanceTracker? = null
            private set

        fun addBeacons(beacons: Map<Identifier, Point>) = apply { beaconsCorners.putAll(beacons) }
        fun addBeacon(beacon: Identifier, point: Point) = apply {
            beaconsCorners.put(beacon, point)
        }
        fun addBeacon(namespace: String, instance: String, x: Double, y: Double) = apply {
            beaconsCorners.put(Identifier(namespace, instance), Point(x,y))
        }
        fun trackCoordinate(tracker: CoordinateTracker) = apply { this.tracker = tracker }
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
            return CoordinateCalculator(beaconsCorners, tracker!!, cacheTime, distanceTracker, calcMethod);
        }
    }


    class Holder(var rrsi: Int, var txPower: Int) {
        var d : Long = Date().time
    }

    var positions =  beaconsCorners.map { doubleArrayOf(it.value.x, it.value.y) }.toTypedArray();

    var distanses: MutableMap<Identifier, LinkedList<Holder>> =  mutableMapOf();

    init {
        beaconsCorners.forEach{distanses.put(it.key, LinkedList())}
    }

    override fun onBeaconDistanceFound(beacon: Identifier, rssi: Int, txPower: Int) {
        if (beaconsCorners.containsKey(beacon)) {
            distanses.get( beacon)?.add(Holder(rssi, txPower))

            val distancesMedium: MutableMap<Identifier, Double> = getMedium()
            val medium = distancesMedium.get(beacon)
            if (distanceTracker != null) {
                distanceTracker!!.onDistanceChange(beacon, calcMethod(rssi.toDouble(), txPower), medium!!)
            }
            print(beacon.instance)
            distanses.get(beacon)?.forEach { print("${it.rrsi},") }
            println("med:$medium")
            recalcCoordinates(distancesMedium)
            val xs = x;
            val ys = y;
            if (xs != null && ys != null) {
                coordTracker.onCoordinateChange(xs,ys)
            }
        }
    }


    fun recalcCoordinates(distancesMedium: MutableMap<Identifier, Double>): MutableMap<Identifier, Double>? {
        var d = DoubleArray(0)
        if (distancesMedium.size == beaconsCorners.size) {
            beaconsCorners.forEach{ d+= distancesMedium.get(it.key) as Double}
            val solver = NonLinearLeastSquaresSolver(TrilaterationFunction(positions, d), LevenbergMarquardtOptimizer())
            val optimum = solver.solve()
            val centroid = optimum.point.toArray()
            val standardDeviation = optimum.getSigma(0.0)
            val covarianceMatrix = optimum.getCovariances(0.0)
            x = centroid[0]
            y = centroid[1]
            return distancesMedium
        } else {
            return null
        }

    }

    private fun getMedium(): MutableMap<Identifier, Double> {
        val now = Date().time
        val distancesMedium: MutableMap<Identifier, Double> = mutableMapOf()
        distanses.forEach { ik ->
            val iterator = ik.value.iterator()
            var rrsiSumm = 0
            var min = -300;
            var txPower: Int? = null;
            while (iterator.hasNext()) {
                val d = iterator.next()
                txPower = d.txPower
                if (now - d.d > cacheTime) {
                    iterator.remove()
                } else {
                    if (d.rrsi > min) {
                        min = d.rrsi
                    }
                    rrsiSumm += d.rrsi
                }
            }
            val mediana: Double
            if (ik.value.size > 1) {
                mediana = calculateMedium(rrsiSumm, min, ik)
            } else {
                mediana = min.toDouble()
            }

            if (rrsiSumm != 0 && txPower != null) {
                distancesMedium.put(ik.key, calcMethod(mediana, txPower))
            }
        }
        return distancesMedium
    }

    private fun calculateMedium(rrsiSumm: Int, min: Int, ik: Map.Entry<Identifier, LinkedList<Holder>>) =
            (((rrsiSumm - min).toDouble() / (ik.value.size - 1)) + min.toDouble()) / 2


}