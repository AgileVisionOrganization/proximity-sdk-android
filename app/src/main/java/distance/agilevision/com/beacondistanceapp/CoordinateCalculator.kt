package distance.agilevision.com.beacondistanceapp

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer


/**
 * @author Andrew Koidan, AgileVision, 25.01.18.
 */
class CoordinateCalculator(var tracker: CoordinateTracker, var beaconsCorners: Map<Identifier, Point>): OnScanResult {

    var positions =  beaconsCorners.map { doubleArrayOf(it.value.x, it.value.y) }.toTypedArray();

    var distanses: Map<Identifier, Double> =  mutableMapOf<Identifier, Double>();

    override fun onBeaconDistanceFound(beacon: Identifier, distance: Double) {
        if (beaconsCorners.containsKey(beacon)) {
            distanses += beacon to distance;
            tracker.onDistanceChange(beacon, distance);
            if (distanses.size == positions.size) {
                val (x,y) = calcDistance()
                tracker.onCoordinateChange(x,y)
            }
        }
    }


    fun calcDistance(): Pair<Double, Double> {
        var d = DoubleArray(0)
        distanses.forEach { d += it.value }
        val solver = NonLinearLeastSquaresSolver(TrilaterationFunction(positions, d), LevenbergMarquardtOptimizer())
        val optimum = solver.solve()
        val centroid = optimum.point.toArray()
        val standardDeviation = optimum.getSigma(0.0)
        val covarianceMatrix = optimum.getCovariances(0.0)
        return Pair(centroid[0],centroid[1])
    }

}