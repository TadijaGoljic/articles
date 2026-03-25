package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.viewmodels.Location
import com.example.articles.viewmodels.LocationPickerViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay


@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun LocationPicker(
    locationPickerViewModel: LocationPickerViewModel = hiltViewModel(),
    onLocationSelected: (Location) -> Unit,
    onCancel: () -> Unit
) {

    val context = LocalContext.current
    val state by locationPickerViewModel.uiState.collectAsState()

    LaunchedEffect(context) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
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
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)

                            controller.setZoom(state.zoomLevel)
                            controller.setCenter(state.selectedLocation)

                            overlays.add(RotationGestureOverlay(this))

                            val mapEventsReceiver = object : MapEventsReceiver {
                                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                    p?.let { geoPoint ->
                                        locationPickerViewModel.updateLocation(geoPoint)
                                        locationPickerViewModel.updateZoom(this@apply.zoomLevelDouble)

                                        overlays.removeAll { it is Marker }

                                        val marker = Marker(this@apply)
                                        marker.position = geoPoint
                                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        marker.title = "Selected Location"
                                        overlays.add(marker)

                                        invalidate()
                                    }
                                    return true
                                }

                                override fun longPressHelper(p: GeoPoint?): Boolean {
                                    return false
                                }
                            }

                            val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                            overlays.add(0, mapEventsOverlay)
                        }
                    },
                    update = { mapView ->
                        mapView.controller.animateTo(state.selectedLocation)
                        mapView.controller.setZoom(state.zoomLevel)
                        mapView.overlays.removeAll { it is Marker }

                        val marker = Marker(mapView)
                        marker.position = state.selectedLocation
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = context.getString(R.string.selected_location)
                        mapView.overlays.add(marker)
                    },
                    modifier = Modifier.fillMaxSize()
                )
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