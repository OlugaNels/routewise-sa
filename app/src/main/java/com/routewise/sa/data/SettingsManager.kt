package com.routewise.sa.data

import android.content.Context
import android.content.SharedPreferences

/** Stores the user's app settings on the phone so they survive closing the app. */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("routewise_settings", Context.MODE_PRIVATE)

    var useKm: Boolean
        get() = prefs.getBoolean(KEY_UNIT_KM, true)
        set(value) = prefs.edit().putBoolean(KEY_UNIT_KM, value).apply()

    fun getToggle(key: String, default: Boolean = true): Boolean = prefs.getBoolean(key, default)

    fun setToggle(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    companion object {
        private const val KEY_UNIT_KM = "unit_km"
        const val KEY_LIVE_TRAFFIC = "live_traffic"
        const val KEY_NIGHT_THEME = "night_theme"
        const val KEY_SPEED_WARNINGS = "speed_warnings"
        const val KEY_TURN_BY_TURN = "turn_by_turn"
        const val KEY_ACCIDENT_WARNINGS = "accident_warnings"
        const val KEY_POLICE_ALERTS = "police_alerts"
        const val KEY_ROADWORK_ALERTS = "roadwork_alerts"
    }
}
