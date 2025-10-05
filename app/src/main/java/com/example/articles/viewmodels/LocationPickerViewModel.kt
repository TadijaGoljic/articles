package com.example.articles.viewmodels

import android.os.Parcelable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.osmdroid.util.GeoPoint
import javax.inject.Inject

@HiltViewModel
class LocationPickerViewModel @Inject constructor(

) : ViewModel() {

    private var state = MutableStateFlow(LocationPickerState())
    val uiState = state

    fun updateLocation(location: GeoPoint) {
        state.update { currentState ->
            currentState.copy(
                selectedLocation = location
            )
        }
    }

    fun updateZoom(zoom: Double) {
        state.update { currentState ->
            currentState.copy(
                zoomLevel = zoom
            )
        }
    }
}

@Parcelize
data class LocationData(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    override fun toString(): String {
        return "${"%.6f".format(latitude)},${"%.6f".format(longitude)}"
    }
}

data class LocationPickerState(
    val selectedLocation: GeoPoint = GeoPoint(44.80567303794035, 20.476393389720563),
    val zoomLevel: Double = 15.0,
)
