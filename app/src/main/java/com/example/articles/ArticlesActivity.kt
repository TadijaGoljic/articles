package com.example.articles

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.example.articles.models.Location
import android.util.Log
import com.example.articles.utils.TriggerSync
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArticlesActivity : ComponentActivity() {

    @Inject lateinit var triggerSync: TriggerSync

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    companion object {
        private const val GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            Log.d(ArticlesApplication.LOG_TAG, "FCM token: $token")
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                triggerSync()
            }
        }
        setContent {
            ArticlesTheme {
                Application()
            }
        }
    }

    private fun openNavigation(location: Location) {
        val uri = buildString {
            append("${GOOGLE_MAPS_BASE_URL}/dir/?")
            append("api=1&")
            append("destination=${location.latitude},${location.longitude}&")
            append("travelmode=driving")
        }.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(GOOGLE_MAPS_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openLink(url: String) =
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

    private fun onSignOut() {
        finish()
        startActivity(Intent(this, AuthenticationActivity::class.java))
    }

    private fun onAccountDelete() = onSignOut()

    @Composable
    fun Application(
        modifier: Modifier = Modifier
    ) {
        ArticlesNavigationFlow(
            onSignOut = ::onSignOut,
            onAccountDelete = ::onAccountDelete,
            onOpenNavigation = ::openNavigation,
            onOpenLink = ::openLink,
            modifier = modifier
        )
    }

}