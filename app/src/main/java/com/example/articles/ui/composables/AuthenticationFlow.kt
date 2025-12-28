package com.example.articles.ui.composables

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.SignInScreen

@Composable
fun AuthenticationNavigationFlow(
    onMoveToBackground: () -> Unit,
    onAuthenticationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    Scaffold { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AuthenticationDestinations.SIGN_IN,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = AuthenticationDestinations.SIGN_IN) {
                SignInScreen(
                    onAuthenticationComplete = onAuthenticationComplete,
                    onMoveToBackground = onMoveToBackground
                )
            }
        }
    }

}

private object AuthenticationDestinations {
    const val SIGN_IN = "sign_in"
}