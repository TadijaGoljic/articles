package com.example.articles.ui.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.SignIn
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.firebase.auth.FirebaseUser

@Composable
fun AuthenticationNavigationFlow(
    onAuthenticationComplete: (FirebaseUser) -> Unit,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val colors = MaterialTheme.colorScheme
    val useDarkIcons = !isSystemInDarkTheme()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colors.surface,
            darkIcons = useDarkIcons
        )
    }

    NavHost(
        navController = navController,
        startDestination = AuthenticationDestinations.SIGN_IN,
        modifier = modifier
    ) {
        composable(route = AuthenticationDestinations.SIGN_IN) {
            SignIn(
                onAuthenticationComplete = onAuthenticationComplete
            )
        }
    }

}

private object AuthenticationDestinations {
    const val SIGN_IN = "sign_in"
}