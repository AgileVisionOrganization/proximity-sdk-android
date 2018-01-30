package com.agilevision.navigator

import android.util.Log
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
        var cacheTime: Int?,
        var distanceTracker: DistanceTracker?,
        var calcMethod: (Double, Int) -> Double,
        var logs: Boolean) : BeaconsTracker {

    var x: Double? = null
    var y: Double? = null

    class Holder(var rrsi: Int, var txPower: Int) {
        var d : Long = Date().time
    }

    var positions =  beaconsCorners.map { doubleArrayOf(it.value.x, it.value.y) }.toTypedArray();

    var distansesCachable: MutableMap<Beacon, LinkedList<Holder>> =  mutableMapOf();
    var distancesSingle: MutableMap<Beacon, Double> =  mutableMapOf();

    init {
        beaconsCorners.forEach{distansesCachable.put(it.key, LinkedList())}
    }

    override fun onBeaconDistanceFound(beacon: Beacon, rssi: Int, txPower: Int) {
        if (beaconsCorners.containsKey(beacon)) {
            val distancesMedium: MutableMap<Beacon, Double>
            if (cacheTime != null) {
                distansesCachable.get(beacon)?.add(Holder(rssi, txPower))
                distancesMedium = getMedium()
            } else {
                distancesSingle.put(beacon, calcMethod(rssi.toDouble(), txPower))
                distancesMedium = distancesSingle
            }

            val medium = distancesMedium.get(beacon)
            if (distanceTracker != null) {
                distanceTracker!!.onDistanceChange(beacon, calcMethod(rssi.toDouble(), txPower), medium!!)
            }
            if (logs) {
                val allRssis: String?
                if (cacheTime != null) {
                    allRssis = ",all rssi"+ distansesCachable.get(beacon)?.joinToString(transform = { it.rrsi.toString() })
                } else {
                    allRssis = ""
                }
                Log.d("CoordinateCalculator", "Found beacon $beacon, rssi: $rssi, txPower, : $txPower $allRssis")
            }
            if (distancesMedium.size == beaconsCorners.size) {
                recalcCoordinates(distancesMedium)
                coordTracker.onCoordinateChange(x!!, y!!)
            }
        }
    }


    fun recalcCoordinates(distancesMedium: MutableMap<Beacon, Double>) {
        var d = DoubleArray(0)
        beaconsCorners.forEach { d += distancesMedium.get(it.key)!! }
        val solver = NonLinearLeastSquaresSolver(TrilaterationFunction(positions, d), LevenbergMarquardtOptimizer())
        val optimum = solver.solve()
        val centroid = optimum.point.toArray()
        x = centroid[0]
        y = centroid[1]
        if (logs) {
            val standardDeviation = optimum.getSigma(0.0)
            val covarianceMatrix = optimum.getCovariances(0.0)
            Log.d("Calculated coord", String.format("{%.2f, %.2f}. Deviation=%s; Covariance=%s", x, y, standardDeviation, covarianceMatrix))
        }
    }

    private fun getMedium(): MutableMap<Beacon, Double> {
        val now = Date().time
        val distancesMedium: MutableMap<Beacon, Double> = mutableMapOf()
        distansesCachable.forEach { ik ->
            val iterator = ik.value.iterator()
            var rrsiSumm = 0
            var min = -300;
            var txPower: Int? = null;
            while (iterator.hasNext()) {
                val d = iterator.next()
                txPower = d.txPower
                if (now - d.d > cacheTime!!) {
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