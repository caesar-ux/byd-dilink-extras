package com.byd.dilink.extras.hazard.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Environment
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.byd.dilink.extras.data.dao.HazardRecord
import com.byd.dilink.extras.data.preferences.HazardPrefsKeys
import com.byd.dilink.extras.data.preferences.hazardPrefs
import com.byd.dilink.extras.data.repository.HazardRepository
import com.byd.dilink.extras.hazard.model.HazardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

data class NearbyHazard(
    val record: HazardRecord,
    val distanceMeters: Double,
    val bearingDeg: Double,
    val directionLabel: String
)

data class HazardUiState(
    val isRecording: Boolean = false,
    val currentLat: Double = 0.0,
    val currentLon: Double = 0.0,
    val currentSpeed: Float = 0f,
    val currentHeading: Float = 0f,
    val nearbyHazards: List<NearbyHazard> = emptyList(),
    val nearbyCount: Int = 0,
    val closestWarning: NearbyHazard? = null,
    val allHazards: List<HazardRecord> = emptyList(),
    val showMoreTypes: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val warningDistanceMeters: Int = 500,
    val warningSoundEnabled: Boolean = true,
    val warningVolume: Float = 0.8f,
    val autoRecord: Boolean = true,
    val hazardExpiryDays: Int = 0,
    val shouldPlayWarning: Boolean = false,
    // Route hazards
    val routeHazards: List<Pair<HazardRecord, Double>> = emptyList(),
    val routeSearchActive: Boolean = false,
    // Filter
    val selectedTypeFilters: Set<HazardType> = emptySet(),
    val searchQuery: String = ""
)

@HiltViewModel
class HazardViewModel @Inject constructor(
    application: Application,
    private val hazardRepository: HazardRepository
) : AndroidViewModel(application), LocationListener, SensorEventListener {

    private val _uiState = MutableStateFlow(HazardUiState())
    val uiState: StateFlow<HazardUiState> = _uiState.asStateFlow()

    private val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var proximityJob: Job? = null
    private val warnedHazardIds = mutableMapOf<Long, Long>() // hazardId -> last warning timestamp
    private val WARNING_DEBOUNCE_MS = 60_000L

    init {
        loadPreferences()
        collectHazards()
        startCompass()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.data.collect { prefs ->
                _uiState.update { state ->
                    state.copy(
                        warningDistanceMeters = prefs[HazardPrefsKeys.WARNING_DISTANCE_METERS] ?: 500,
                        warningSoundEnabled = prefs[HazardPrefsKeys.WARNING_SOUND_ENABLED] ?: true,
                        warningVolume = prefs[HazardPrefsKeys.WARNING_VOLUME] ?: 0.8f,
                        autoRecord = prefs[HazardPrefsKeys.AUTO_RECORD] ?: true,
                        hazardExpiryDays = prefs[HazardPrefsKeys.HAZARD_EXPIRY_DAYS] ?: 0
                    )
                }
            }
        }
    }

    private fun collectHazards() {
        viewModelScope.launch {
            hazardRepository.getAll().collect { hazards ->
                _uiState.update { it.copy(allHazards = hazards) }
            }
        }
    }

    private fun startCompass() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        _uiState.update { it.copy(hasLocationPermission = true) }
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // 1 second
                5f,    // 5 meters
                this
            )
            // Try to get last known location
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)?.let {
                updateLocation(it)
            }
        } catch (_: Exception) { }
    }

    fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) { }
    }

    fun toggleRecording() {
        val newRecording = !_uiState.value.isRecording
        _uiState.update { it.copy(isRecording = newRecording) }
        if (newRecording) {
            startProximityEngine()
        } else {
            proximityJob?.cancel()
        }
    }

    fun startRecording() {
        if (!_uiState.value.isRecording) {
            _uiState.update { it.copy(isRecording = true) }
            startProximityEngine()
        }
    }

    private fun startProximityEngine() {
        proximityJob?.cancel()
        proximityJob = viewModelScope.launch {
            while (true) {
                checkProximity()
                delay(2000L) // Check every 2 seconds
            }
        }
    }

    private suspend fun checkProximity() {
        val state = _uiState.value
        if (state.currentLat == 0.0 && state.currentLon == 0.0) return

        val warningDist = state.warningDistanceMeters.toDouble()
        val radarRadius = 5000.0 // 5km radar

        val hazardsInRadius = hazardRepository.getHazardsWithinRadius(
            state.currentLat, state.currentLon, radarRadius
        )

        val nearbyList = hazardsInRadius.map { (record, distance) ->
            val bearing = HazardRepository.bearing(
                state.currentLat, state.currentLon,
                record.latitude, record.longitude
            )
            NearbyHazard(
                record = record,
                distanceMeters = distance,
                bearingDeg = bearing,
                directionLabel = HazardRepository.directionLabel(bearing)
            )
        }

        val closest = nearbyList.firstOrNull { it.distanceMeters <= warningDist }

        // Check if we should trigger a warning sound
        var shouldWarn = false
        if (closest != null && state.warningSoundEnabled) {
            val lastWarned = warnedHazardIds[closest.record.id] ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastWarned > WARNING_DEBOUNCE_MS) {
                shouldWarn = true
                warnedHazardIds[closest.record.id] = now
            }
        }

        // Clean old debounce entries
        val now = System.currentTimeMillis()
        warnedHazardIds.entries.removeAll { now - it.value > WARNING_DEBOUNCE_MS * 2 }

        _uiState.update { s ->
            s.copy(
                nearbyHazards = nearbyList,
                nearbyCount = nearbyList.size,
                closestWarning = closest,
                shouldPlayWarning = shouldWarn
            )
        }
    }

    fun clearWarningSound() {
        _uiState.update { it.copy(shouldPlayWarning = false) }
    }

    fun addHazard(type: HazardType, notes: String? = null) {
        val state = _uiState.value
        if (state.currentLat == 0.0 && state.currentLon == 0.0) return

        viewModelScope.launch {
            val record = HazardRecord(
                type = type.name,
                latitude = state.currentLat,
                longitude = state.currentLon,
                heading = state.currentHeading,
                speed = state.currentSpeed,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                confirmed = 1
            )
            hazardRepository.insert(record)
        }
    }

    fun deleteHazard(record: HazardRecord) {
        viewModelScope.launch {
            hazardRepository.delete(record)
        }
    }

    fun toggleShowMoreTypes() {
        _uiState.update { it.copy(showMoreTypes = !it.showMoreTypes) }
    }

    // Filter functions
    fun toggleTypeFilter(type: HazardType) {
        _uiState.update { state ->
            val current = state.selectedTypeFilters.toMutableSet()
            if (type in current) current.remove(type) else current.add(type)
            state.copy(selectedTypeFilters = current)
        }
    }

    fun clearTypeFilters() {
        _uiState.update { it.copy(selectedTypeFilters = emptySet()) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun getFilteredHazards(): List<HazardRecord> {
        val state = _uiState.value
        var filtered = state.allHazards

        if (state.selectedTypeFilters.isNotEmpty()) {
            val typeNames = state.selectedTypeFilters.map { it.name }.toSet()
            filtered = filtered.filter { it.type in typeNames }
        }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.lowercase()
            filtered = filtered.filter {
                it.notes?.lowercase()?.contains(q) == true ||
                        it.type.lowercase().contains(q)
            }
        }

        // Sort by distance from current location
        return if (state.currentLat != 0.0 || state.currentLon != 0.0) {
            filtered.sortedBy {
                HazardRepository.haversineDistance(
                    state.currentLat, state.currentLon,
                    it.latitude, it.longitude
                )
            }
        } else {
            filtered.sortedByDescending { it.timestamp }
        }
    }

    // Route hazards
    fun searchRouteHazards(
        startLat: Double, startLon: Double,
        endLat: Double, endLon: Double,
        corridorWidth: Double = 1000.0
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(routeSearchActive = true) }
            val results = hazardRepository.getHazardsAlongCorridor(
                startLat, startLon, endLat, endLon, corridorWidth
            )
            _uiState.update { it.copy(routeHazards = results, routeSearchActive = false) }
        }
    }

    fun clearRouteHazards() {
        _uiState.update { it.copy(routeHazards = emptyList()) }
    }

    // Settings
    fun updateWarningDistance(meters: Int) {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.edit { prefs ->
                prefs[HazardPrefsKeys.WARNING_DISTANCE_METERS] = meters
            }
        }
    }

    fun updateWarningSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.edit { prefs ->
                prefs[HazardPrefsKeys.WARNING_SOUND_ENABLED] = enabled
            }
        }
    }

    fun updateWarningVolume(volume: Float) {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.edit { prefs ->
                prefs[HazardPrefsKeys.WARNING_VOLUME] = volume
            }
        }
    }

    fun updateAutoRecord(enabled: Boolean) {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.edit { prefs ->
                prefs[HazardPrefsKeys.AUTO_RECORD] = enabled
            }
        }
    }

    fun updateHazardExpiryDays(days: Int) {
        viewModelScope.launch {
            getApplication<Application>().hazardPrefs.edit { prefs ->
                prefs[HazardPrefsKeys.HAZARD_EXPIRY_DAYS] = days
            }
            // Clean up expired hazards if expiry is set
            if (days > 0) {
                val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
                hazardRepository.deleteOlderThan(cutoff)
            }
        }
    }

    // Export/Import
    fun exportToJson(): String {
        var result = "[]"
        viewModelScope.launch {
            val hazards = hazardRepository.getAllOnce()
            val jsonArray = JSONArray()
            hazards.forEach { h ->
                val obj = JSONObject().apply {
                    put("type", h.type)
                    put("latitude", h.latitude)
                    put("longitude", h.longitude)
                    put("heading", h.heading.toDouble())
                    put("speed", h.speed.toDouble())
                    put("timestamp", h.timestamp)
                    put("notes", h.notes ?: "")
                    put("confirmed", h.confirmed)
                }
                jsonArray.put(obj)
            }
            result = jsonArray.toString(2)
        }
        return result
    }

    fun exportToFile() {
        viewModelScope.launch {
            try {
                val hazards = hazardRepository.getAllOnce()
                val jsonArray = JSONArray()
                hazards.forEach { h ->
                    val obj = JSONObject().apply {
                        put("type", h.type)
                        put("latitude", h.latitude)
                        put("longitude", h.longitude)
                        put("heading", h.heading.toDouble())
                        put("speed", h.speed.toDouble())
                        put("timestamp", h.timestamp)
                        put("notes", h.notes ?: "")
                        put("confirmed", h.confirmed)
                    }
                    jsonArray.put(obj)
                }
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                dir.mkdirs()
                val file = File(dir, "hazards_export_${System.currentTimeMillis()}.json")
                file.writeText(jsonArray.toString(2))
            } catch (_: Exception) { }
        }
    }

    fun importFromJson(jsonString: String) {
        viewModelScope.launch {
            try {
                val jsonArray = JSONArray(jsonString)
                val records = (0 until jsonArray.length()).map { i ->
                    val obj = jsonArray.getJSONObject(i)
                    HazardRecord(
                        type = obj.getString("type"),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        heading = obj.getDouble("heading").toFloat(),
                        speed = obj.getDouble("speed").toFloat(),
                        timestamp = obj.getLong("timestamp"),
                        notes = obj.optString("notes").ifEmpty { null },
                        confirmed = obj.optInt("confirmed", 1)
                    )
                }
                hazardRepository.insertAll(records)
            } catch (_: Exception) { }
        }
    }

    fun clearAllHazards() {
        viewModelScope.launch {
            hazardRepository.deleteAll()
        }
    }

    // LocationListener
    override fun onLocationChanged(location: Location) {
        updateLocation(location)
    }

    private fun updateLocation(location: Location) {
        val speedKmh = location.speed * 3.6f
        _uiState.update { state ->
            state.copy(
                currentLat = location.latitude,
                currentLon = location.longitude,
                currentSpeed = speedKmh,
                currentHeading = if (location.hasBearing()) location.bearing else state.currentHeading
            )
        }
        // Auto-record: start recording when speed > 10 km/h
        val state = _uiState.value
        if (state.autoRecord && !state.isRecording && speedKmh > 10f) {
            startRecording()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
    override fun onProviderEnabled(provider: String) { }
    override fun onProviderDisabled(provider: String) { }

    // SensorEventListener
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            val azimuthDeg = ((Math.toDegrees(orientation[0].toDouble()) + 360) % 360).toFloat()
            _uiState.update { it.copy(currentHeading = azimuthDeg) }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onCleared() {
        super.onCleared()
        stopLocationUpdates()
        sensorManager.unregisterListener(this)
        proximityJob?.cancel()
    }
}
