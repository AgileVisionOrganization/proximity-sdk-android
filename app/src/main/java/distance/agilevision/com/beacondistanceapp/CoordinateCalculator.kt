package distance.agilevision.com.beacondistanceapp

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import java.util.*


/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
class CoordinateCalculator(var tracker: CoordinateTracker, var beaconsCorners: Map<Identifier, Point>): OnScanResult {

    var x: Double? = null
    var y: Double? = null

    val SIGNAL_LOSS_1M = 41;
    val CACHE_TIME = 4000

    fun calculateDistance(rssi: Double, txPower: Int): Double {
        return Math.pow(10.0, ( txPower.toDouble() - SIGNAL_LOSS_1M - rssi) / (10 * 2))
    }

    class Holder(var rrsi: Int, var txPower: Int) {
        var d : Long = Date().time
    }


    fun calculateDistancev2(rssi: Double, txPower: Int): Double {
        val ratio = rssi * 1.0 / (txPower - SIGNAL_LOSS_1M);
        if (ratio < 1.0) {
            return Math.pow(ratio, 10.0);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
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

            tracker.onDistanceChange(beacon, calculateDistance(rssi.toDouble(), txPower), medium)
            print(beacon.instance)
            distanses.get(beacon)?.forEach { print("${it.rrsi},") }
            println("med:$medium")
            recalcCoordinates(distancesMedium)
            val xs = x;
            val ys = y;
            if (xs != null && ys != null) {
                tracker.onCoordinateChange(xs,ys)
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
                if (now - d.d > CACHE_TIME) {
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
                distancesMedium.put(ik.key, calculateDistance(mediana, txPower))
            }
        }
        return distancesMedium
    }

    private fun calculateMedium(rrsiSumm: Int, min: Int, ik: Map.Entry<Identifier, LinkedList<Holder>>) =
            (((rrsiSumm - min).toDouble() / (ik.value.size - 1)) + min.toDouble()) / 2

}