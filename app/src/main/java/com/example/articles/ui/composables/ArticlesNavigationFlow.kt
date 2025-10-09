package com.example.articles.ui.composables

import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.ArticlesList
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.viewmodels.LocationData
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@SuppressLint("RestrictedApi")
@Composable
fun ArticlesNavigationFlow(
    moveTaskToBackground: () -> Unit,
    onOpenNavigation: (LocationData) -> Unit,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val colors = MaterialTheme.colorScheme
    val useDarkIcons = !isSystemInDarkTheme()

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = colors.background,
            darkIcons = useDarkIcons
        )
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.ARTICLES_LIST,
        modifier = modifier
    ) {
        composable(route = Destinations.ARTICLES_LIST) {
            ArticlesList(
                onFloatingActionButtonClick = {
                    navController.navigate(Destinations.ADD_NEW_ARTICLE)
                },
                onOpenNavigation = { locationData ->
                    onOpenNavigation(locationData)
                },
                onBackClick = moveTaskToBackground
            )
        }
        composable(route = Destinations.ADD_NEW_ARTICLE) {
            AddNewArticle(
                onBackButtonClicked = {
                    if (navController.currentBackStackEntry?.destination?.route != Destinations.ARTICLES_LIST) {
                        navController.popBackStack()
                    }
                },
                onLocationPickerClick = {
                    navController.navigate(Destinations.PICK_LOCATION)
                },
                navController = navController,
                onFinish = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = Destinations.PICK_LOCATION) {
            LocationPicker(
                onLocationSelected = { location ->
                    navController.previousBackStackEntry?.savedStateHandle["location"] = location
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
    }

}

object Destinations {
    const val ARTICLES_LIST = "articles_list"
    const val ADD_NEW_ARTICLE = "add_new_article"
    const val PICK_LOCATION = "pick_location"
}