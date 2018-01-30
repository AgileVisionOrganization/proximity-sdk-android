package io.agilevision.proximity.indoor

import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val positions = arrayOf(doubleArrayOf(5.0, -6.0), doubleArrayOf(13.0, -15.0), doubleArrayOf(21.0, -3.0), doubleArrayOf(12.4, -21.2))
        val distances = doubleArrayOf(8.06, 13.97, 23.32, 15.31)


        val solver = NonLinearLeastSquaresSolver(TrilaterationFunction(positions, distances), LevenbergMarquardtOptimizer())
        val optimum = solver.solve()

        val centroid = optimum.point.toArray()
// error and geometry information; may throw SingularMatrixException depending the threshold argument provided
        val standardDeviation = optimum.getSigma(0.0)
        val covarianceMatrix = optimum.getCovariances(0.0)
        print(covarianceMatrix)

    }
}
