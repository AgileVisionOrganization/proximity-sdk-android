package com.agilevision.navigator

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.util.*


/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
class CoordinateCalculator (
        var beaconsCorners: Map<Beacon, XYPoint>,
        var coordTracker: CoordinateTracker,
        var cacheTime: Int,
        var distanceTracker: DistanceTracker?,
        var calcMethod: (Double, Int) -> Double,
        var logs: Boolean) : BeaconsTracker {

    var x: Double? = null
    var y: Double? = null

    class Holder(var rrsi: Int, var txPower: Int) {
        var d : Long = Date().time
    }

    var positions =  beaconsCorners.map { doubleArrayOf(it.value.x, it.value.y) }.toTypedArray();

    var distanses: MutableMap<Beacon, LinkedList<Holder>> =  mutableMapOf();

    init {
        beaconsCorners.forEach{distanses.put(it.key, LinkedList())}
    }

    override fun onBeaconDistanceFound(beacon: Beacon, rssi: Int, txPower: Int) {
        if (beaconsCorners.containsKey(beacon)) {
            distanses.get(beacon)?.add(Holder(rssi, txPower))
            val distancesMedium: MutableMap<Beacon, Double> = getMedium()
            val medium = distancesMedium.get(beacon)
            if (distanceTracker != null) {
                distanceTracker!!.onDistanceChange(beacon, calcMethod(rssi.toDouble(), txPower), medium!!)
            }
            if (logs) {
                print("Called onBeaconDistanceFound with $beacon, rssi: $rssi, txPower, : $txPower")
                distanses.get(beacon)?.forEach { print("${it.rrsi},") }
            }
            recalcCoordinates(distancesMedium)
            val xs = x;
            val ys = y;
            if (xs != null && ys != null) {
                coordTracker.onCoordinateChange(xs,ys)
            }
        }
    }


    fun recalcCoordinates(distancesMedium: MutableMap<Beacon, Double>): MutableMap<Beacon, Double>? {
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

    private fun getMedium(): MutableMap<Beacon, Double> {
        val now = Date().time
        val distancesMedium: MutableMap<Beacon, Double> = mutableMapOf()
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

    private fun calculateMedium(rrsiSumm: Int, min: Int, ik: Map.Entry<Beacon, LinkedList<Holder>>) =
            (((rrsiSumm - min).toDouble() / (ik.value.size - 1)) + min.toDouble()) / 2


}