package com.example.articles.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication.Companion.LOG_TAG
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private val DEFAULT_LAT_LNG = LatLng(44.80567303794035, 20.476393389720563)
private const val SEARCH_DEBOUNCE_MS = 300L
private const val SELECTED_PLACE_ZOOM = 17f
private const val CURRENT_LOCATION_ZOOM = 15f

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val placesClient: PlacesClient,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val explicitLat = savedStateHandle.get<Double>("latitude")
    private val explicitLon = savedStateHandle.get<Double>("longitude")
    private val hasExplicitLocation = explicitLat != null && explicitLon != null
    private val initialLocation = if (hasExplicitLocation) LatLng(explicitLat!!, explicitLon!!) else DEFAULT_LAT_LNG

    private var state = MutableStateFlow(
        LocationPickerState(
            selectedLocation = initialLocation,
            requestLocationPermission = !hasExplicitLocation && !hasLocationPermission()
        )
    )
    val uiState = state

    private var sessionToken = AutocompleteSessionToken.newInstance()
    private var searchJob: Job? = null

    init {
        if (!hasExplicitLocation && hasLocationPermission()) {
            fetchCurrentLocation()
        }
    }

    fun handleAction(action: LocationPickerAction) {
        when (action) {
            is LocationPickerAction.UpdateLocation -> state.update { it.copy(selectedLocation = action.location) }
            is LocationPickerAction.UpdateZoom -> state.update { it.copy(zoomLevel = action.zoom) }
            is LocationPickerAction.UpdateSearchQuery -> updateSearchQuery(action.query)
            is LocationPickerAction.SelectPrediction -> selectPrediction(action.prediction)
            is LocationPickerAction.LocationPermissionResult -> onLocationPermissionResult(action.granted)
        }
    }

    private fun onLocationPermissionResult(granted: Boolean) {
        state.update { it.copy(requestLocationPermission = false) }
        if (granted) fetchCurrentLocation()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocation() {
        if (!hasLocationPermission() || !isLocationEnabled()) return
        viewModelScope.launch {
            try {
                val location = LocationServices.getFusedLocationProviderClient(context)
                    .lastLocation.await() ?: return@launch
                state.update {
                    it.copy(
                        selectedLocation = LatLng(location.latitude, location.longitude),
                        zoomLevel = CURRENT_LOCATION_ZOOM
                    )
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to get current location: ${e.message}")
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun updateSearchQuery(query: String) {
        state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()

        if (query.isBlank()) {
            state.update { it.copy(predictions = emptyList()) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(sessionToken)
                .build()
            try {
                val response = placesClient.findAutocompletePredictions(request).await()
                state.update { it.copy(predictions = response.autocompletePredictions) }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Place search failed: ${e.message}")
                state.update { it.copy(predictions = emptyList()) }
            }
        }
    }

    private fun selectPrediction(prediction: AutocompletePrediction) {
        viewModelScope.launch {
            val request = FetchPlaceRequest.newInstance(prediction.placeId, listOf(Place.Field.LOCATION))
            try {
                val place = placesClient.fetchPlace(request).await().place
                val location = place.location
                if (location != null) {
                    state.update {
                        it.copy(
                            selectedLocation = location,
                            zoomLevel = SELECTED_PLACE_ZOOM,
                            searchQuery = prediction.getPrimaryText(null).toString(),
                            predictions = emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to fetch place details: ${e.message}")
            }
            sessionToken = AutocompleteSessionToken.newInstance()
        }
    }

}

data class LocationPickerState(
    val selectedLocation: LatLng = DEFAULT_LAT_LNG,
    val zoomLevel: Float = 15f,
    val searchQuery: String = "",
    val predictions: List<AutocompletePrediction> = emptyList(),
    val requestLocationPermission: Boolean = false,
)

sealed interface LocationPickerAction {
    data class UpdateLocation(val location: LatLng) : LocationPickerAction
    data class UpdateZoom(val zoom: Float) : LocationPickerAction
    data class UpdateSearchQuery(val query: String) : LocationPickerAction
    data class SelectPrediction(val prediction: AutocompletePrediction) : LocationPickerAction
    data class LocationPermissionResult(val granted: Boolean) : LocationPickerAction
}
