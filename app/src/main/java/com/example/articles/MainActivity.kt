package com.example.articles

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import dagger.hilt.android.AndroidEntryPoint
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArticlesTheme {
                ArticlesNavigationFlow()
            }
        }
    }
}

//@Composable
//fun LocationPickerApp(
//    modifier: Modifier = Modifier,
//) {
//
//    var showMap by remember { mutableStateOf(false) }
//    var selectedLocation by remember { mutableStateOf<LocationData?>(null) }
//
//    Surface(
//        modifier = modifier.fillMaxSize(),
//        color = MaterialTheme.colorScheme.background
//    ) {
//        if (showMap) {
//            MapPickerScreen(
//                onLocationSelected = { location ->
//                    selectedLocation = location
//                    showMap = false
//                },
//                onCancel = { showMap = false }
//            )
//        } else {
//            MainScreen(
//                selectedLocation = selectedLocation,
//                onSelectLocationClick = { showMap = true }
//            )
//        }
//    }
//}
//
//@Composable
//fun MainScreen(
//    selectedLocation: LocationData?,
//    onSelectLocationClick: () -> Unit,
//    modifier: Modifier = Modifier,
//) {
//    Column(
//        modifier = modifier
//            .fillMaxSize()
//            .padding(16.dp),
//        horizontalAlignment = Alignment.CenterHorizontally,
//        verticalArrangement = Arrangement.Center
//    ) {
//        if (selectedLocation != null) {
//            Text(
//                text = "📍 Selected Location",
//                style = MaterialTheme.typography.headlineSmall,
//                color = MaterialTheme.colorScheme.primary
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//            Text(
//                text = "Latitude: ${"%.6f".format(selectedLocation.latitude)}",
//                style = MaterialTheme.typography.bodyLarge
//            )
//            Text(
//                text = "Longitude: ${"%.6f".format(selectedLocation.longitude)}",
//                style = MaterialTheme.typography.bodyLarge
//            )
//            Spacer(modifier = Modifier.height(32.dp))
//        } else {
//            Text(
//                text = "No location selected",
//                style = MaterialTheme.typography.bodyLarge,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Spacer(modifier = Modifier.height(32.dp))
//        }
//
//        Button(
//            onClick = onSelectLocationClick,
//            modifier = Modifier
//                .fillMaxWidth(0.8f)
//                .height(56.dp)
//        ) {
//            Text(
//                text = if (selectedLocation == null) "📌 Select Location" else "🔄 Change Location",
//                style = MaterialTheme.typography.bodyLarge
//            )
//        }
//    }
//}
