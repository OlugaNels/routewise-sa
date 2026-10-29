package com.routewise.sa.util

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Distance maths on latitude/longitude pairs. */
object Geo {
    private const val EARTH_RADIUS_M = 6_371_000.0

    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * EARTH_RADIUS_M * asin(min(1.0, sqrt(a)))
    }
}

/**
 * Follows a driver along a route line. Give it the points of the route and the point where each step starts,
 * then call [update] with every GPS fix to learn how far along the route the driver is, which step they are on,
 * and whether they have left the route.
 */
class RouteTracker(
    private val lats: DoubleArray,
    private val lngs: DoubleArray,
    stepStartLats: DoubleArray,
    stepStartLngs: DoubleArray
) {
    data class Progress(
        val offRouteMeters: Double,
        val fractionDone: Double,
        /** The step the driver is travelling on (the step whose start they have passed). */
        val stepIndex: Int,
        /** Distance along the route to the start of the next step, or to the end on the last step. */
        val metersToNextStep: Double,
        val metersToEnd: Double
    )

    private val count = lats.size
    private val cumulative = DoubleArray(count)
    private val stepVertex = IntArray(stepStartLats.size)
    private var lastSegment = 0

    init {
        require(count >= 2) { "A route needs at least two points" }
        for (i in 1 until count) {
            cumulative[i] = cumulative[i - 1] + Geo.haversineMeters(lats[i - 1], lngs[i - 1], lats[i], lngs[i])
        }
        var from = 0
        for (s in stepVertex.indices) {
            var best = from
            var bestDistance = Double.MAX_VALUE
            for (i in from until count) {
                val d = Geo.haversineMeters(stepStartLats[s], stepStartLngs[s], lats[i], lngs[i])
                if (d < bestDistance) { bestDistance = d; best = i }
            }
            stepVertex[s] = best
            from = best
        }
    }

    val totalMeters: Double get() = cumulative[count - 1]

    private class Hit(val segment: Int, val t: Double, val distance: Double)

    private fun nearest(lat: Double, lng: Double, firstSegment: Int, lastSeg: Int): Hit {
        // work in metres around the point, which is accurate enough over a few hundred metres
        val metersPerDegLat = 111_320.0
        val metersPerDegLng = 111_320.0 * cos(lat * PI / 180.0)
        var best = Hit(firstSegment, 0.0, Double.MAX_VALUE)
        for (i in firstSegment..lastSeg) {
            val ax = (lngs[i] - lng) * metersPerDegLng
            val ay = (lats[i] - lat) * metersPerDegLat
            val bx = (lngs[i + 1] - lng) * metersPerDegLng
            val by = (lats[i + 1] - lat) * metersPerDegLat
            val dx = bx - ax
            val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0.0) 0.0 else max(0.0, min(1.0, -(ax * dx + ay * dy) / len2))
            val d = hypot(ax + t * dx, ay + t * dy)
            if (d < best.distance) best = Hit(i, t, d)
        }
        return best
    }

    fun update(lat: Double, lng: Double): Progress {
        val lastIndex = count - 2
        // look near the last position first, so a road that crosses itself does not confuse the tracker
        var hit = nearest(lat, lng, max(0, lastSegment - 3), min(lastIndex, lastSegment + 300))
        if (hit.distance > 60.0) {
            val global = nearest(lat, lng, 0, lastIndex)
            if (global.distance < hit.distance) hit = global
        }
        lastSegment = hit.segment

        val segLength = cumulative[hit.segment + 1] - cumulative[hit.segment]
        val along = cumulative[hit.segment] + hit.t * segLength
        var step = 0
        val reachedVertex = if (hit.t >= 0.999) hit.segment + 1 else hit.segment
        for (s in stepVertex.indices) if (stepVertex[s] <= reachedVertex) step = s
        val nextVertex = if (step + 1 < stepVertex.size) stepVertex[step + 1] else count - 1
        val toNext = max(0.0, cumulative[nextVertex] - along)
        val toEnd = max(0.0, totalMeters - along)
        val fraction = if (totalMeters == 0.0) 1.0 else min(1.0, max(0.0, along / totalMeters))
        return Progress(hit.distance, fraction, step, toNext, toEnd)
    }
}
