package com.routewise.sa.service

import android.content.Context
import android.util.Log
import com.routewise.sa.data.SettingsManager
import com.routewise.sa.data.model.IncidentEntity
import com.routewise.sa.util.HazardFilter

/**
 * Tells the driver when another driver reports a new hazard (added in version 1.2.0).
 *
 * MainActivity feeds it the live incident list. The first list only records what already exists,
 * so opening the app never floods the driver with old alerts. After that, each new incident of a type
 * the driver switched on in Settings produces one heads-up notification.
 */
class HazardNotifier(private val context: Context) {

    private val settings = SettingsManager(context)
    private val knownIds = mutableSetOf<String>()
    private var seeded = false

    fun onIncidents(incidents: List<IncidentEntity>) {
        val prefs = settings.hazardPreferences()

        if (!seeded) {
            // First list after the app opens: remember it, do not notify
            knownIds.addAll(incidents.map { it.id })
            seeded = true
            Log.d(TAG, "Seeded with ${knownIds.size} existing incidents")
            return
        }

        val fresh = HazardFilter.newIncidents(knownIds, incidents, prefs)
        knownIds.addAll(incidents.map { it.id })

        if (!settings.getToggle(SettingsManager.KEY_HAZARD_NOTIFICATIONS)) {
            Log.d(TAG, "${fresh.size} new incident(s) but hazard notifications are switched off")
            return
        }

        // Cap at three so a burst of reports does not bury the driver
        for (incident in fresh.take(MAX_PER_BATCH)) {
            val place = listOf(incident.roadName, incident.city).filter { it.isNotBlank() }.joinToString(", ")
            AlertNotifications.show(
                context,
                title = "⚠️ ${incident.type} reported",
                message = if (place.isEmpty()) "A driver reported a hazard near you." else "Reported on $place",
                lat = incident.latitude,
                lng = incident.longitude
            )
        }
    }

    private companion object {
        const val TAG = "HazardNotifier"
        const val MAX_PER_BATCH = 3
    }
}
