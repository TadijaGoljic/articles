package com.example.articles

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.example.articles.viewmodels.LocationData
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArticlesTheme {
                Application()
            }
        }
    }

    fun openNavigation(context: Context, location: LocationData) {
        val uri = (
            "${GOOGLE_MAPS_BASE_URL}/dir/?" +
            "api=1&" +
            "destination=${location.latitude},${location.longitude}&" +
            "travelmode=driving"
        ).toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(GOOGLE_MAPS_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun moveTaskToBackground() {
        moveTaskToBack(true)
    }

    @Composable
    fun Application(
        modifier: Modifier = Modifier
    ) {
        ArticlesNavigationFlow(
            onOpenNavigation = { location ->
                openNavigation(
                    context = this@MainActivity,
                    location = location
                )
            },
            modifier = modifier,
            moveTaskToBackground = { moveTaskToBackground() }
        )
    }

}