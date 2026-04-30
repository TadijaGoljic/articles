package com.example.articles.ui.composables

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.ArticlesList
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.ui.composables.screens.SettingsScreen
import com.example.articles.viewmodels.AddNewArticleViewModel.Companion.KEY_LOCATION
import com.example.articles.models.Location

@SuppressLint("RestrictedApi")
@Composable
fun ArticlesNavigationFlow(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    moveTaskToBackground: () -> Unit,
    onOpenNavigation: (Location) -> Unit,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = ArticlesDestinations.ARTICLES_LIST,
        modifier = modifier
    ) {
        composable(route = ArticlesDestinations.ARTICLES_LIST) {
            ArticlesList(
                onFloatingActionButtonClick = {
                    navController.navigate(ArticlesDestinations.ADD_NEW_ARTICLE)
                },
                onOpenNavigation = { locationData ->
                    onOpenNavigation(locationData)
                },
                onBackClick = moveTaskToBackground,
                onOpenSettings = {
                    navController.navigate(ArticlesDestinations.SETTINGS)
                }
            )
        }
        composable(route = ArticlesDestinations.ADD_NEW_ARTICLE) {
            AddNewArticle(
                onBackButtonClicked = {
                    if (navController.currentBackStackEntry?.destination?.route != ArticlesDestinations.ARTICLES_LIST) {
                        navController.popBackStack()
                    }
                },
                onLocationPickerClick = {
                    navController.navigate(ArticlesDestinations.PICK_LOCATION)
                },
                navController = navController,
                onFinish = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = ArticlesDestinations.PICK_LOCATION) {
            LocationPicker(
                onLocationSelected = { location ->
                    navController.previousBackStackEntry?.savedStateHandle[KEY_LOCATION] = location
                    navController.popBackStack()
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }
        composable(route = ArticlesDestinations.SETTINGS) {
            SettingsScreen(
                onSignOut = onSignOut,
                onBackClick = {
                    navController.popBackStack()
                },
                onAccountDelete = onAccountDelete
            )
        }
    }

}

private object ArticlesDestinations {
    const val ARTICLES_LIST = "articles_list"
    const val ADD_NEW_ARTICLE = "add_new_article"
    const val PICK_LOCATION = "pick_location"
    const val SETTINGS = "settings"
}