package com.routewise.sa.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/** Turns the data a routing service sends for each step into text a driver can read. */
object NavigationText {

    private fun direction(modifier: String?): String? = when (modifier?.lowercase(Locale.ROOT)) {
        "left" -> "left"
        "right" -> "right"
        "slight left" -> "slight left"
        "slight right" -> "slight right"
        "sharp left" -> "sharp left"
        "sharp right" -> "sharp right"
        "straight" -> "straight"
        "uturn" -> "U-turn"
        else -> null
    }

    private fun side(modifier: String?): String? = when {
        modifier == null -> null
        modifier.contains("left", ignoreCase = true) -> "left"
        modifier.contains("right", ignoreCase = true) -> "right"
        else -> null
    }

    fun compass(bearing: Int): String {
        val names = listOf("north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest")
        val normal = ((bearing % 360) + 360) % 360
        return names[((normal + 22.5) / 45.0).toInt() % 8]
    }

    fun ordinal(n: Int): String = when {
        n % 100 in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

    /** Builds one instruction such as "Turn left onto Joe Slovo Drive". */
    fun instruction(
        type: String?,
        modifier: String?,
        road: String?,
        exit: Int? = null,
        bearingAfter: Int? = null
    ): String {
        val name = road?.trim().orEmpty()
        val onto = if (name.isEmpty()) "" else " onto $name"
        val on = if (name.isEmpty()) "" else " on $name"
        val dir = direction(modifier)
        val t = type?.lowercase(Locale.ROOT)

        return when (t) {
            "depart" -> if (bearingAfter != null) "Head ${compass(bearingAfter)}$on" else "Start driving$on"
            "arrive" -> when (side(modifier)) {
                "left" -> "Arrive at your destination, on the left"
                "right" -> "Arrive at your destination, on the right"
                else -> "Arrive at your destination"
            }
            "turn", "end of road" -> {
                val prefix = if (t == "end of road") "At the end of the road, " else ""
                val body = when (dir) {
                    "U-turn" -> "make a U-turn$on"
                    "straight" -> "continue straight$on"
                    null -> "continue$on"
                    else -> "turn $dir$onto"
                }
                if (prefix.isEmpty()) body.replaceFirstChar { it.uppercase() } else prefix + body
            }
            "new name" -> "Continue$on"
            "continue" -> when (dir) {
                null, "straight" -> "Continue$on"
                "U-turn" -> "Make a U-turn$on"
                else -> "Continue $dir$on"
            }
            "merge" -> if (side(modifier) != null) "Merge ${side(modifier)}$onto" else "Merge$onto"
            "on ramp" -> "Take the ramp$onto"
            "off ramp" -> if (side(modifier) != null) "Take the exit on the ${side(modifier)}$onto" else "Take the exit$onto"
            "fork" -> if (side(modifier) != null) "Keep ${side(modifier)}$onto" else "Continue$on"
            "roundabout", "rotary" ->
                if (exit != null && exit > 0) "At the roundabout, take the ${ordinal(exit)} exit$onto"
                else "Enter the roundabout$onto"
            "roundabout turn" -> if (dir != null) "At the roundabout, turn $dir$onto" else "At the roundabout, continue$onto"
            "exit roundabout", "exit rotary" -> "Exit the roundabout$onto"
            else -> "Continue$on"
        }
    }

    /** "350 m", "1.2 km" or, when [useKm] is false, "800 ft", "0.7 mi". */
    fun distance(meters: Int, useKm: Boolean = true): String {
        val m = meters.coerceAtLeast(0)
        if (useKm) {
            return when {
                m < 1000 -> "${roundTo(m, if (m < 100) 10 else 50)} m"
                m < 10_000 -> String.format(Locale.US, "%.1f km", m / 1000.0)
                else -> "${(m / 1000.0).roundToInt()} km"
            }
        }
        val feet = m * 3.28084
        return if (feet < 1000) "${roundTo(feet.roundToInt(), 50)} ft"
        else String.format(Locale.US, "%.1f mi", m / 1609.344)
    }

    private fun roundTo(value: Int, step: Int): Int = ((value + step / 2) / step) * step

    /** "1 h 05 min" or "18 min". */
    fun duration(seconds: Int): String {
        val minutes = (seconds.coerceAtLeast(0) + 30) / 60
        return if (minutes >= 60) String.format(Locale.US, "%d h %02d min", minutes / 60, minutes % 60) else "$minutes min"
    }

    /** Arrival time as clock text, for example "17:35". */
    fun arrivalClock(nowMillis: Long, remainingSeconds: Int, zone: TimeZone = TimeZone.getDefault()): String {
        val format = SimpleDateFormat("HH:mm", Locale.US)
        format.timeZone = zone
        return format.format(Date(nowMillis + remainingSeconds.coerceAtLeast(0) * 1000L))
    }
}
