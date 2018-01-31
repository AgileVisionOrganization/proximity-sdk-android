package com.agilevision.navigator

import io.agilevision.proximity.indoor.Beacon
import io.agilevision.proximity.indoor.CoordinateBuilder
import io.agilevision.proximity.indoor.CoordinateTracker
import io.agilevision.proximity.indoor.XYPoint
import junit.framework.Assert
import org.hamcrest.number.IsCloseTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class CoordinateCalculatorTest {

    lateinit var cb: CoordinateBuilder;
    val a = Beacon("00112233445566778899", "000000000000")
    val b = Beacon("00112233445566778899", "111111000000")
    val c = Beacon("00112233445566778899", "111111111111")
    val d = Beacon("00112233445566778899", "000000111111")

    @Before
    fun createBuilder() {
        cb = CoordinateBuilder()
                .addBeacon(a, XYPoint(0.0, 0.0))
                .addBeacon(b, XYPoint(3.07, 0.0))
                .addBeacon(c, XYPoint(3.07, 5.66))
                .addBeacon(d, XYPoint(0.0, 5.66))

    }

    @Test
    fun coordinatorTest() {

        val handler = object: CoordinateTracker {
            var x: Double? = null
            var y: Double? = null
            var called: Int = 0
            override fun onCoordinateChange(x: Double, y: Double) {
                called++
                this.x = x;
                this.y = y;
            }

        }

        val tracker = cb.trackCoordinate(handler).build()
        tracker.onBeaconDistanceFound(c, -57, -5)
        Assert.assertEquals(handler.called, 0)
        tracker.onBeaconDistanceFound(d, -56, -9)
        Assert.assertEquals(handler.called, 0)
        tracker.onBeaconDistanceFound(b, -66, 0)
        Assert.assertEquals(handler.called, 0)
        tracker.onBeaconDistanceFound(a, -63, -13)
        Assert.assertEquals(handler.called, 1)
        assertThat(handler.x, IsCloseTo(-1.0, 0.1))
        assertThat(handler.y, IsCloseTo(4.0, 0.1))
        tracker.onBeaconDistanceFound(a, -64, -13)
        tracker.onBeaconDistanceFound(a, -62, -13)
        tracker.onBeaconDistanceFound(b, -61, 0)
        tracker.onBeaconDistanceFound(c, -61, -5)
        tracker.onBeaconDistanceFound(c, -61, -5)
        tracker.onBeaconDistanceFound(d, -61, -9)
        tracker.onBeaconDistanceFound(d, -61, -9)
        Assert.assertEquals(handler.called, 8)
        assertThat(handler.x, IsCloseTo(-1.7, 0.1))
        assertThat(handler.y, IsCloseTo(3.5, 0.1))

    }
}