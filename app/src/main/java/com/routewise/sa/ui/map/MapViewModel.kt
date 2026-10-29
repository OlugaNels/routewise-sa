package com.routewise.sa.ui.map

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.routewise.sa.data.model.IncidentEntity
import com.routewise.sa.data.model.RouteStepEntity
import com.routewise.sa.data.repository.CalculatedRoute
import com.routewise.sa.data.repository.DirectionsRepository
import com.routewise.sa.data.repository.IncidentRepository
import com.routewise.sa.data.repository.RouteRepository
import com.routewise.sa.util.RouteTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.URLEncoder

/** A place found by name. */
data class GeoResult(val point: GeoPoint, val label: String)

/** Real distance and time to a destination, shown on the place card before navigation starts. */
data class TripPreview(
    val destinationName: String,
    val address: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val mainRoad: String
)

/** Where the driver is on the active route, worked out from GPS. */
data class TripProgress(
    val metersToNextManeuver: Int,
    val remainingMeters: Int,
    val remainingSeconds: Int,
    val fractionDone: Double,
    val offRoute: Boolean,
    val arrived: Boolean
)

class MapViewModel(
    private val routeRepository: RouteRepository,
    private val incidentRepository: IncidentRepository
) : ViewModel() {

    private val _activeRoute = MutableLiveData<CalculatedRoute?>()
    val activeRoute: LiveData<CalculatedRoute?> = _activeRoute

    private val _currentStepIndex = MutableLiveData<Int>(0)
    val currentStepIndex: LiveData<Int> = _currentStepIndex

    private val _isVoiceMuted = MutableLiveData<Boolean>(false)
    val isVoiceMuted: LiveData<Boolean> = _isVoiceMuted

    private val _incidents = MutableLiveData<List<IncidentEntity>>()
    val incidents: LiveData<List<IncidentEntity>> = _incidents

    private val _focusedIncident = MutableLiveData<IncidentEntity?>()
    val focusedIncident: LiveData<IncidentEntity?> = _focusedIncident

    private val _routeError = MutableLiveData<String?>()
    /** A message to show when a route could not be made. */
    val routeError: LiveData<String?> = _routeError

    private val _tripPreview = MutableLiveData<TripPreview?>()
    val tripPreview: LiveData<TripPreview?> = _tripPreview

    private val _progress = MutableLiveData<TripProgress?>()
    val progress: LiveData<TripProgress?> = _progress

    /** True when the route that was just posted replaces one the driver left, so the map should not jump to a new view. */
    var lastRouteWasReroute = false
        private set

    /** Latest GPS fix, kept up to date by the map screen. */
    var userLocation: GeoPoint? = null

    private val directionsRepository = DirectionsRepository()
    private val httpClient = OkHttpClient()

    private var tracker: RouteTracker? = null
    private var routeDestination: GeoPoint? = null
    private var offRouteCount = 0
    private var rerouting = false
    private var lastRerouteAt = 0L

    init {
        loadIncidents()
    }

    fun focusIncidentOnMap(incident: IncidentEntity) {
        _focusedIncident.value = incident
    }

    fun clearFocusedIncident() {
        _focusedIncident.value = null
    }

    fun upvoteIncident(id: String) {
        viewModelScope.launch {
            incidentRepository.upvote(id)
        }
    }

    fun refreshIncidents() {
        loadIncidents()
    }

    private fun loadIncidents() {
        viewModelScope.launch {
            incidentRepository.getIncidents(null).collectLatest {
                _incidents.postValue(it)
            }
        }
    }

    fun clearRouteError() {
        _routeError.value = null
    }

    private fun isCurrentLocationText(text: String): Boolean {
        val lower = text.trim().lowercase()
        return lower.isEmpty() || lower.startsWith("my location") || lower.startsWith("current location")
    }

    fun planTrip(origin: String, destination: String, avoidTolls: Boolean) {
        viewModelScope.launch {
            val origPoint = if (isCurrentLocationText(origin)) {
                userLocation
            } else {
                geocode(origin)?.point
            }
            if (origPoint == null) {
                _routeError.postValue(
                    if (isCurrentLocationText(origin)) "Your location is not available yet. Turn on location and try again."
                    else "Could not find the start location \"$origin\"."
                )
                return@launch
            }
            planTripFromPoint(origPoint, destination, avoidTolls)
        }
    }

    fun planTripWithCoordinates(originPoint: GeoPoint, destination: String, avoidTolls: Boolean) {
        viewModelScope.launch {
            planTripFromPoint(originPoint, destination, avoidTolls)
        }
    }

    private suspend fun planTripFromPoint(originPoint: GeoPoint, destination: String, avoidTolls: Boolean) {
        val dest = geocode(destination)
        if (dest == null) {
            _routeError.postValue("Could not find \"$destination\". Check the spelling and your internet connection.")
            return
        }
        planTripInternal(originPoint, dest.point, destination, avoidTolls, isReroute = false)
    }

    private suspend fun planTripInternal(
        originPoint: GeoPoint,
        destPoint: GeoPoint,
        destinationName: String,
        avoidTolls: Boolean,
        isReroute: Boolean
    ) {
        val result = directionsRepository.getRoadRoute(originPoint, destPoint)
        val dirRoute = result.getOrNull()
        if (dirRoute == null || dirRoute.polylinePoints.size < 2 || dirRoute.steps.isEmpty()) {
            _routeError.postValue("Could not get a road route to $destinationName. Check your internet connection and try again.")
            return
        }

        val routeId = "route_${System.currentTimeMillis()}"
        val stepEntities = dirRoute.steps.mapIndexed { index, step ->
            val next = dirRoute.steps.getOrNull(index + 1)
            RouteStepEntity(
                routeId = routeId,
                stepIndex = index,
                instruction = step.instruction,
                roadName = step.roadName,
                distanceMeters = step.distanceMeters.toLong(),
                durationSeconds = step.durationSec.toLong(),
                startLat = step.startLat,
                startLng = step.startLng,
                endLat = next?.startLat ?: destPoint.latitude,
                endLng = next?.startLng ?: destPoint.longitude,
                maneuver = step.maneuver
            )
        }

        val calculated = CalculatedRoute(
            routeId = routeId,
            originName = "Current Location",
            destinationName = destinationName,
            totalDistanceMeters = dirRoute.totalDistanceMeters.toLong(),
            totalDurationSeconds = dirRoute.totalDurationSeconds.toLong(),
            overviewPolylinePoints = "",
            pathPoints = dirRoute.polylinePoints,
            steps = stepEntities,
            waypoints = emptyList(),
            speedLimit = if (dirRoute.totalDistanceMeters > 20000) 120 else 60
        )

        routeDestination = destPoint
        buildTracker(calculated)
        offRouteCount = 0
        lastRouteWasReroute = isReroute
        _progress.postValue(null)
        _activeRoute.postValue(calculated)
        _currentStepIndex.postValue(0)
    }

    private fun buildTracker(route: CalculatedRoute) {
        tracker = if (route.pathPoints.size >= 2) {
            RouteTracker(
                DoubleArray(route.pathPoints.size) { route.pathPoints[it].latitude },
                DoubleArray(route.pathPoints.size) { route.pathPoints[it].longitude },
                DoubleArray(route.steps.size) { route.steps[it].startLat },
                DoubleArray(route.steps.size) { route.steps[it].startLng }
            )
        } else null
    }

    /** Looks up the real distance and time to a place, for the place card. */
    fun previewDestination(origin: GeoPoint, destinationName: String) {
        _tripPreview.value = null
        viewModelScope.launch {
            val place = geocode(destinationName)
            if (place == null) {
                _routeError.postValue("Could not find \"$destinationName\". Check the spelling and your internet connection.")
                return@launch
            }
            val route = directionsRepository.getRoadRoute(origin, place.point).getOrNull()
            if (route == null) {
                _routeError.postValue("Could not get a road route to $destinationName.")
                return@launch
            }
            val longest = route.steps.maxByOrNull { it.distanceMeters }
            _tripPreview.postValue(
                TripPreview(
                    destinationName = destinationName,
                    address = place.label,
                    distanceMeters = route.totalDistanceMeters,
                    durationSeconds = route.totalDurationSeconds,
                    mainRoad = longest?.roadName ?: ""
                )
            )
        }
    }

    /** Called with every GPS fix while a route is active. */
    fun onLocation(point: GeoPoint) {
        userLocation = point
        val route = _activeRoute.value ?: return
        val tracker = tracker ?: return
        val p = tracker.update(point.latitude, point.longitude)

        val total = route.totalDistanceMeters.toDouble()
        val remaining = (p.metersToEnd / tracker.totalMeters.coerceAtLeast(1.0)) * total
        val remainingSec = (p.metersToEnd / tracker.totalMeters.coerceAtLeast(1.0)) * route.totalDurationSeconds
        val offRoute = p.offRouteMeters > OFF_ROUTE_METERS
        val arrived = p.metersToEnd < ARRIVAL_METERS

        _progress.value = TripProgress(
            metersToNextManeuver = p.metersToNextStep.toInt(),
            remainingMeters = remaining.toInt(),
            remainingSeconds = remainingSec.toInt(),
            fractionDone = p.fractionDone,
            offRoute = offRoute,
            arrived = arrived
        )

        // the step only moves forward on GPS, so the Next and Previous buttons still work for a demonstration
        val current = _currentStepIndex.value ?: 0
        if (!arrived && p.stepIndex > current) _currentStepIndex.value = p.stepIndex

        if (offRoute && !arrived) {
            offRouteCount++
            val now = System.currentTimeMillis()
            if (offRouteCount >= OFF_ROUTE_FIXES && !rerouting && now - lastRerouteAt > REROUTE_COOLDOWN_MS) {
                reroute(point, route.destinationName)
            }
        } else {
            offRouteCount = 0
        }
    }

    private fun reroute(from: GeoPoint, destinationName: String) {
        val dest = routeDestination ?: return
        rerouting = true
        lastRerouteAt = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                planTripInternal(from, dest, destinationName, avoidTolls = false, isReroute = true)
            } finally {
                rerouting = false
            }
        }
    }

    private suspend fun geocode(place: String): GeoResult? = withContext(Dispatchers.IO) {
        val text = place.trim()
        if (text.isEmpty()) return@withContext null
        nominatim(text) ?: nominatim("$text, South Africa") ?: photon(text)
    }

    private fun shortLabel(display: String): String =
        display.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3).joinToString(", ")

    private fun nominatim(query: String): GeoResult? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&countrycodes=za"
            val request = Request.Builder().url(url).header("User-Agent", "RouteWiseSA/1.0 (com.routewise.sa)").build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val arr = JSONArray(response.body?.string() ?: return null)
                if (arr.length() == 0) return null
                val obj = arr.getJSONObject(0)
                val lat = obj.optString("lat").toDoubleOrNull() ?: return null
                val lon = obj.optString("lon").toDoubleOrNull() ?: return null
                GeoResult(GeoPoint(lat, lon), shortLabel(obj.optString("display_name", query)))
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun photon(query: String): GeoResult? {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder().url("https://photon.komoot.io/api/?q=$encoded&limit=1").header("User-Agent", "RouteWiseSA/1.0 (com.routewise.sa)").build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val features = JSONObject(response.body?.string() ?: return null).optJSONArray("features") ?: return null
                if (features.length() == 0) return null
                val feature = features.getJSONObject(0)
                val coords = feature.getJSONObject("geometry").getJSONArray("coordinates")
                val props = feature.optJSONObject("properties")
                val label = listOfNotNull(props?.optString("name"), props?.optString("city"), props?.optString("state"))
                    .filter { it.isNotEmpty() }.joinToString(", ")
                GeoResult(GeoPoint(coords.getDouble(1), coords.getDouble(0)), label.ifEmpty { query })
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setCalculatedRoute(route: CalculatedRoute) {
        lastRouteWasReroute = false
        buildTracker(route)
        _activeRoute.value = route
        _currentStepIndex.value = 0
    }

    fun nextStep() {
        val current = _currentStepIndex.value ?: 0
        val max = (_activeRoute.value?.steps?.size ?: 1) - 1
        if (current < max) {
            _currentStepIndex.value = current + 1
        }
    }

    fun prevStep() {
        val current = _currentStepIndex.value ?: 0
        if (current > 0) {
            _currentStepIndex.value = current - 1
        }
    }

    fun toggleVoice() {
        _isVoiceMuted.value = !(_isVoiceMuted.value ?: false)
    }

    fun clearRoute() {
        tracker = null
        routeDestination = null
        offRouteCount = 0
        _progress.value = null
        _activeRoute.value = null
        _currentStepIndex.value = 0
    }

    private companion object {
        const val OFF_ROUTE_METERS = 60.0
        const val OFF_ROUTE_FIXES = 4
        const val ARRIVAL_METERS = 40.0
        const val REROUTE_COOLDOWN_MS = 20_000L
    }
}
