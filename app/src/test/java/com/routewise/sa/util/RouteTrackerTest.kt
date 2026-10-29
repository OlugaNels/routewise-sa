package com.routewise.sa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteTrackerTest {

    // A route that runs due east along latitude -26.0 for about 2.2 km, with a step starting every ~730 m.
    private val lng0 = 28.0
    private val lats = DoubleArray(23) { -26.0 }
    private val lngs = DoubleArray(23) { lng0 + it * 0.001 }
    private fun tracker() = RouteTracker(
        lats, lngs,
        doubleArrayOf(-26.0, -26.0, -26.0, -26.0),
        doubleArrayOf(lng0, lng0 + 0.007, lng0 + 0.014, lng0 + 0.022)
    )

    @Test fun startOfRouteIsOnStepZero() {
        val p = tracker().update(-26.0, lng0)
        assertEquals(0, p.stepIndex)
        assertEquals(0.0, p.fractionDone, 0.01)
        assertTrue(p.offRouteMeters < 1.0)
    }

    @Test fun passingTheSecondStepMovesToStepOne() {
        val p = tracker().update(-26.0, lng0 + 0.0075)
        assertEquals(1, p.stepIndex)
    }

    @Test fun distanceToNextStepShrinksAsTheDriverMoves() {
        val t = tracker()
        val early = t.update(-26.0, lng0 + 0.0010).metersToNextStep
        val later = t.update(-26.0, lng0 + 0.0050).metersToNextStep
        assertTrue(later < early)
        // about 0.002 degrees of longitude is roughly 200 m at this latitude
        assertEquals(200.0, later, 30.0)
    }

    @Test fun halfwayIsAboutHalf() {
        val p = tracker().update(-26.0, lng0 + 0.011)
        assertEquals(0.5, p.fractionDone, 0.02)
    }

    @Test fun endOfRouteIsArrival() {
        val p = tracker().update(-26.0, lng0 + 0.022)
        assertEquals(3, p.stepIndex)
        assertTrue(p.metersToEnd < 5.0)
        assertEquals(1.0, p.fractionDone, 0.01)
    }

    @Test fun sidewaysOffsetIsReportedAsOffRoute() {
        // 0.001 degrees of latitude is about 111 m north of the line
        val p = tracker().update(-25.999, lng0 + 0.005)
        assertEquals(111.0, p.offRouteMeters, 8.0)
    }

    @Test fun nearbyPointIsNotOffRoute() {
        val p = tracker().update(-25.99985, lng0 + 0.005)
        assertTrue(p.offRouteMeters < 25.0)
    }

    @Test fun haversineKnownDistance() {
        // one degree of latitude is about 111.2 km
        assertEquals(111_195.0, Geo.haversineMeters(0.0, 0.0, 1.0, 0.0), 200.0)
    }

    @Test fun totalLengthMatchesTheLine() {
        assertEquals(2_200.0, tracker().totalMeters, 40.0)
    }
}
