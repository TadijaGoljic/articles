package com.example.articles

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.IntervalTree
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.articles.ui.composables.AuthenticationNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthenticationActivity : ComponentActivity() {

    companion object {
        const val USER = "user"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                val user = Firebase.auth.currentUser
                user?.getIdToken(false)?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onAuthenticationComplete(user)
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

    private fun onAuthenticationComplete(user: FirebaseUser) {
        val intent = Intent(this, ArticlesActivity::class.java).apply {
            putExtra(USER, user)
        }
        startActivity(intent)
        finish()
    }

    @Composable
    private fun Authentication(
        modifier: Modifier = Modifier
    ) {
        AuthenticationNavigationFlow(
            onAuthenticationComplete = { onAuthenticationComplete(it) },
            modifier = modifier
        )
    }

}