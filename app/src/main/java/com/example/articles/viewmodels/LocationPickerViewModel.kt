package com.example.articles.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

private val DEFAULT_GEO_POINT = GeoPoint(44.80567303794035, 20.476393389720563)

@HiltViewModel
class LocationPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialLocation = run {
        val lat = savedStateHandle.get<Double>("latitude")
        val lon = savedStateHandle.get<Double>("longitude")
        if (lat != null && lon != null) GeoPoint(lat, lon)
        else DEFAULT_GEO_POINT
    }

    private var state = MutableStateFlow(LocationPickerState(selectedLocation = initialLocation))
    val uiState = state

    fun handleAction(action: LocationPickerAction) {
        when (action) {
            is LocationPickerAction.UpdateLocation -> state.update { it.copy(selectedLocation = action.location) }
            is LocationPickerAction.UpdateZoom -> state.update { it.copy(zoomLevel = action.zoom) }
        }
    }

}

data class LocationPickerState(
    val selectedLocation: GeoPoint = DEFAULT_GEO_POINT,
    val zoomLevel: Double = 15.0,
)

sealed interface LocationPickerAction {
    data class UpdateLocation(val location: GeoPoint) : LocationPickerAction
    data class UpdateZoom(val zoom: Double) : LocationPickerAction
}
