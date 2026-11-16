package com.routewise.sa.util

import com.routewise.sa.data.model.IncidentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for the Settings switches that decide which hazards are shown and notified (version 1.2.0). */
class HazardFilterTest {

    private fun incident(id: String, type: String) = IncidentEntity(
        id = id, type = type, roadName = "N1", city = "Johannesburg", province = "Gauteng",
        latitude = -26.2, longitude = 28.0, timestamp = 0L, reportedBy = "t",
        description = "", severity = "low"
    )

    private val all = HazardFilter.Preferences()
    private val sample = listOf(
        incident("1", "Accident"), incident("2", "Police Trap"), incident("3", "Roadworks"),
        incident("4", "Road Closure"), incident("5", "Pothole")
    )

    @Test fun everythingShownWhenAllSwitchesAreOn() =
        assertEquals(5, HazardFilter.visibleOnMap(sample, all).size)

    @Test fun nothingOnMapWhenHazardLayerIsOff() =
        assertTrue(HazardFilter.visibleOnMap(sample, all.copy(showOnMap = false)).isEmpty())

    @Test fun feedStillListsIncidentsWhenOnlyTheMapLayerIsOff() =
        assertEquals(5, HazardFilter.visibleInFeed(sample, all.copy(showOnMap = false)).size)

    @Test fun accidentSwitchOffHidesAccidents() {
        val shown = HazardFilter.visibleInFeed(sample, all.copy(accidents = false))
        assertFalse(shown.any { it.type == "Accident" })
        assertEquals(4, shown.size)
    }

    @Test fun policeSwitchOffHidesPoliceTraps() =
        assertFalse(HazardFilter.visibleInFeed(sample, all.copy(police = false)).any { it.type == "Police Trap" })

    @Test fun roadworkSwitchOffHidesRoadworksAndClosures() {
        val shown = HazardFilter.visibleInFeed(sample, all.copy(roadworks = false))
        assertEquals(listOf("1", "2", "5"), shown.map { it.id })
    }

    @Test fun potholesAreAlwaysShown() =
        assertTrue(HazardFilter.isTypeEnabled("Pothole", HazardFilter.Preferences(false, false, false, false)))

    @Test fun typeMatchingIgnoresCase() =
        assertFalse(HazardFilter.isTypeEnabled("ACCIDENT", all.copy(accidents = false)))

    @Test fun newIncidentsSkipsKnownIds() {
        val fresh = HazardFilter.newIncidents(setOf("1", "2", "3"), sample, all)
        assertEquals(listOf("4", "5"), fresh.map { it.id })
    }

    @Test fun newIncidentsRespectsTypeSwitches() {
        val fresh = HazardFilter.newIncidents(emptySet(), sample, all.copy(accidents = false, police = false))
        assertEquals(listOf("3", "4", "5"), fresh.map { it.id })
    }
}
