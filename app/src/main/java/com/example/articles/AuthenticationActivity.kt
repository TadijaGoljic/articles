package com.example.articles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.articles.ui.composables.AuthenticationNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.initialize
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthenticationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Firebase.initialize(this)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                Firebase.auth.currentUser?.getIdToken(false)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onAuthenticationComplete()
                    }
                }
            }
        }
        setContent {
            ArticlesTheme {
                Authentication()
            }
        }
    }

    private fun onAuthenticationComplete() {
        startActivity(Intent(this, ArticlesActivity::class.java))
        finish()
    }

    private fun onMoveToBackground() =
        moveTaskToBack(true)

    @Composable
    private fun Authentication(
        modifier: Modifier = Modifier
    ) {
        AuthenticationNavigationFlow(
            onAuthenticationComplete = { onAuthenticationComplete() },
            onMoveToBackground = { onMoveToBackground() },
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(top = 15.dp)
        )
    }

}