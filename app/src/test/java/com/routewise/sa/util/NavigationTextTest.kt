package com.routewise.sa.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

class NavigationTextTest {

    @Test fun turnLeftNamesTheRoad() =
        assertEquals("Turn left onto Joe Slovo Drive", NavigationText.instruction("turn", "left", "Joe Slovo Drive"))

    @Test fun slightRightTurn() =
        assertEquals("Turn slight right onto M1", NavigationText.instruction("turn", "slight right", "M1"))

    @Test fun uTurn() =
        assertEquals("Make a U-turn on Main Road", NavigationText.instruction("turn", "uturn", "Main Road"))

    @Test fun departUsesCompassDirection() =
        assertEquals("Head south on Von Weilligh Street", NavigationText.instruction("depart", null, "Von Weilligh Street", bearingAfter = 174))

    @Test fun arriveOnTheRight() =
        assertEquals("Arrive at your destination, on the right", NavigationText.instruction("arrive", "right", ""))

    @Test fun roundaboutExit() =
        assertEquals("At the roundabout, take the 2nd exit onto Oak Avenue", NavigationText.instruction("roundabout", "right", "Oak Avenue", exit = 2))

    @Test fun endOfRoad() =
        assertEquals("At the end of the road, turn right onto Rivonia Road", NavigationText.instruction("end of road", "right", "Rivonia Road"))

    @Test fun offRampWithSide() =
        assertEquals("Take the exit on the left onto R21", NavigationText.instruction("off ramp", "slight left", "R21"))

    @Test fun mergeOntoHighway() =
        assertEquals("Merge onto N1", NavigationText.instruction("merge", "straight", "N1"))

    @Test fun unnamedRoadLeavesOutTheName() =
        assertEquals("Turn left", NavigationText.instruction("turn", "left", ""))

    @Test fun unknownTypeStillGivesSomething() =
        assertEquals("Continue on Main Road", NavigationText.instruction("something new", null, "Main Road"))

    @Test fun nullTypeStillGivesSomething() =
        assertEquals("Continue", NavigationText.instruction(null, null, null))

    @Test fun compassPoints() {
        assertEquals("north", NavigationText.compass(0))
        assertEquals("north", NavigationText.compass(359))
        assertEquals("east", NavigationText.compass(90))
        assertEquals("southwest", NavigationText.compass(225))
    }

    @Test fun ordinals() {
        assertEquals("1st", NavigationText.ordinal(1))
        assertEquals("2nd", NavigationText.ordinal(2))
        assertEquals("3rd", NavigationText.ordinal(3))
        assertEquals("4th", NavigationText.ordinal(4))
        assertEquals("11th", NavigationText.ordinal(11))
    }

    @Test fun shortDistancesAreInMetres() {
        assertEquals("30 m", NavigationText.distance(32))
        assertEquals("350 m", NavigationText.distance(347))
    }

    @Test fun longDistancesAreInKilometres() {
        assertEquals("1.2 km", NavigationText.distance(1234))
        assertEquals("57 km", NavigationText.distance(57_300))
    }

    @Test fun negativeDistanceIsZero() = assertEquals("0 m", NavigationText.distance(-5))

    @Test fun imperialDistances() {
        assertEquals("100 ft", NavigationText.distance(30, useKm = false))
        assertEquals("0.6 mi", NavigationText.distance(1000, useKm = false))
    }

    @Test fun durations() {
        assertEquals("18 min", NavigationText.duration(1080))
        assertEquals("1 h 05 min", NavigationText.duration(3900))
        assertEquals("0 min", NavigationText.duration(-10))
    }

    @Test fun arrivalClockAddsTheRemainingTime() {
        val utc = TimeZone.getTimeZone("UTC")
        // 2026-10-19 15:00:00 UTC plus 35 minutes
        val now = 1_792_422_000_000L
        assertEquals("15:35", NavigationText.arrivalClock(now, 35 * 60, utc))
    }
}
