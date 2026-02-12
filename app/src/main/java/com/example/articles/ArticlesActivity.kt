package com.example.articles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import com.example.articles.repositories.ArticleRepository
import com.example.articles.sync.SyncManager
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.example.articles.viewmodels.Location
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ArticlesActivity : ComponentActivity() {

    companion object {
        private const val GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps"
    }

    @Inject lateinit var articleRepository: ArticleRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                WorkManager.getInstance(this@ArticlesActivity).enqueue(SyncManager.sync())
            }
        }
        setContent {
            ArticlesTheme {
                Application()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            articleRepository.deleteAll()
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

    private fun moveTaskToBackground() =
        moveTaskToBack(true)

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
            onOpenNavigation = { location ->
                openNavigation(location = location)
            },
            moveTaskToBackground = {
                moveTaskToBackground()
            },
            onSignOut = {
                onSignOut()
            },
            onAccountDelete = {
                onAccountDelete()
            },
            modifier = modifier
        )
    }

}