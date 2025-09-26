package com.example.articles.composables

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.articles.composables.screens.AddNewArticle
import com.example.articles.composables.screens.ArticlesList
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun ArticlesNavigationFlow(
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

    Scaffold(
        containerColor = colors.surface
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.ARTICLES_LIST,
            modifier = modifier.padding(innerPadding)
        ) {
            composable(route = Destinations.ARTICLES_LIST) {
                ArticlesList(
                    onFloatingActionButtonClick = {
                        navController.navigate(Destinations.ADD_NEW_ARTICLE)
                    }
                )
            }
            composable(route = Destinations.ADD_NEW_ARTICLE) {
                AddNewArticle(
                    onBackButtonClicked = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

}

object Destinations {
    const val ARTICLES_LIST = "articles_list"
    const val ADD_NEW_ARTICLE = "add_new_article"
}