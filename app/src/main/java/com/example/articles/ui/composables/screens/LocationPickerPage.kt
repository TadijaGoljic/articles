package com.example.articles.ui.composables.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.Location
import com.example.articles.viewmodels.LocationPickerAction
import com.example.articles.viewmodels.LocationPickerViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState

@Composable
fun LocationPicker(
    locationPickerViewModel: LocationPickerViewModel = hiltViewModel(),
    onLocationSelected: (Location) -> Unit,
    onCancel: () -> Unit
) {

    val state by locationPickerViewModel.uiState.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(state.selectedLocation, state.zoomLevel)
    }
    val markerState = rememberUpdatedMarkerState(position = state.selectedLocation)

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPickerViewModel.handleAction(LocationPickerAction.LocationPermissionResult(granted))
    }

    LaunchedEffect(state.requestLocationPermission) {
        if (state.requestLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    LaunchedEffect(state.selectedLocation, state.zoomLevel) {
        markerState.position = state.selectedLocation
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(state.selectedLocation, state.zoomLevel)
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        locationPickerViewModel.handleAction(LocationPickerAction.UpdateLocation(latLng))
                        locationPickerViewModel.handleAction(
                            LocationPickerAction.UpdateZoom(cameraPositionState.position.zoom)
                        )
                    }
                ) {
                    Marker(
                        state = markerState,
                        title = stringResource(id = R.string.selected_location)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = {
                            locationPickerViewModel.handleAction(LocationPickerAction.UpdateSearchQuery(it))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
                        placeholder = { Text(text = stringResource(id = R.string.search_for_a_place)) },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        locationPickerViewModel.handleAction(
                                            LocationPickerAction.UpdateSearchQuery("")
                                        )
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        }
                    )

                    if (state.predictions.isNotEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            LazyColumn {
                                items(state.predictions, key = { it.placeId }) { prediction ->
                                    PredictionRow(
                                        prediction = prediction,
                                        onClick = {
                                            locationPickerViewModel.handleAction(
                                                LocationPickerAction.SelectPrediction(prediction)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Text(
                text = stringResource(id = R.string.tap_on_the_map),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = {
                        onLocationSelected(
                            Location(
                                latitude = state.selectedLocation.latitude,
                                longitude = state.selectedLocation.longitude
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(id = R.string.select_location))
                }
            }
        }
    }
}

@Composable
private fun PredictionRow(
    prediction: AutocompletePrediction,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = prediction.getPrimaryText(null).toString(),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = prediction.getSecondaryText(null).toString(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
