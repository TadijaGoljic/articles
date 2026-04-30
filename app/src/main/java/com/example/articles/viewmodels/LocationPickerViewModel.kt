package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import com.example.articles.models.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor(

) : ViewModel() {

    private var state = MutableStateFlow(LocationPickerState())
    val uiState = state

    fun handleAction(action: LocationPickerAction) {
        when (action) {
            is LocationPickerAction.UpdateLocation -> state.update { it.copy(selectedLocation = action.location) }
            is LocationPickerAction.UpdateZoom -> state.update { it.copy(zoomLevel = action.zoom) }
        }
    }

}

data class LocationPickerState(
    val selectedLocation: GeoPoint = GeoPoint(44.80567303794035, 20.476393389720563),
    val zoomLevel: Double = 15.0,
)

sealed interface LocationPickerAction {
    data class UpdateLocation(val location: GeoPoint) : LocationPickerAction
    data class UpdateZoom(val zoom: Double) : LocationPickerAction
}
