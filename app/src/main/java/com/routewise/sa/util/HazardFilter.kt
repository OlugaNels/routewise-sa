package com.routewise.sa.util

import com.routewise.sa.data.model.IncidentEntity

/**
 * Decides which incidents a driver wants to see, based on the switches in Settings.
 * Pure Kotlin (no Android classes), so it is unit tested on the JVM in HazardFilterTest.
 *
 * Added in version 1.2.0 so that the alert switches in Settings change what the map and the
 * alerts feed show while the app is running.
 */
object HazardFilter {

    /** The switches in the "Hazard alerts" section of Settings. */
    data class Preferences(
        val showOnMap: Boolean = true,
        val accidents: Boolean = true,
        val police: Boolean = true,
        val roadworks: Boolean = true
    )

    private fun isAccident(type: String) = type.contains("accident", ignoreCase = true)
    private fun isPolice(type: String) = type.contains("police", ignoreCase = true)
    private fun isRoadwork(type: String) =
        type.contains("roadwork", ignoreCase = true) ||
            type.contains("road work", ignoreCase = true) ||
            type.contains("closure", ignoreCase = true)

    /** True when an incident of this [type] is allowed by the driver's switches. */
    fun isTypeEnabled(type: String, prefs: Preferences): Boolean = when {
        isAccident(type) -> prefs.accidents
        isPolice(type) -> prefs.police
        isRoadwork(type) -> prefs.roadworks
        else -> true // potholes, flooding, traffic and other hazards are always relevant
    }

    /** Incidents the driver has chosen to see in the alerts feed. */
    fun visibleInFeed(incidents: List<IncidentEntity>, prefs: Preferences): List<IncidentEntity> =
        incidents.filter { isTypeEnabled(it.type, prefs) }

    /** Incidents to draw on the map: nothing when the "Live hazard layer" switch is off. */
    fun visibleOnMap(incidents: List<IncidentEntity>, prefs: Preferences): List<IncidentEntity> =
        if (!prefs.showOnMap) emptyList() else visibleInFeed(incidents, prefs)

    /**
     * Incidents that appeared since the last check and that the driver wants to be told about.
     * [knownIds] holds every incident id already seen, so each incident notifies only once.
     */
    fun newIncidents(
        knownIds: Set<String>,
        current: List<IncidentEntity>,
        prefs: Preferences
    ): List<IncidentEntity> =
        current.filter { it.id !in knownIds && isTypeEnabled(it.type, prefs) }
}
