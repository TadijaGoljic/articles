package com.example.articles.ui.composables

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.example.articles.models.UILocation
import com.example.articles.ui.composables.screens.AddNewArticle
import com.example.articles.ui.composables.screens.ArchivedGroupsPage
import com.example.articles.ui.composables.screens.InvitationsPage
import com.example.articles.ui.composables.screens.LocationPicker
import com.example.articles.ui.composables.screens.SettingsScreen
import com.example.articles.ui.composables.screens.GroupDetailsPage
import com.example.articles.ui.composables.screens.GroupsListPage
import com.example.articles.viewmodels.AddNewArticleAction
import com.example.articles.viewmodels.AddNewArticleViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Composable
fun ArticlesNavigationFlow(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    newIntentFlow: Flow<Intent>,
    modifier: Modifier = Modifier
) {

    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        newIntentFlow.collect { intent ->
            navController.handleDeepLink(intent)
        }
    }

    NavHost(
        navController = navController,
        startDestination = ArticlesDestinations.GroupsListDestination,
        modifier = modifier
    ) {
        composable<ArticlesDestinations.GroupsListDestination> {
            GroupsListPage(
                onSettingsClick = {
                    navController.navigate(ArticlesDestinations.SettingsDestination)
                },
                onGroupClick = { group ->
                    navController.navigate(ArticlesDestinations.GroupDetailsDestination(groupId = group.id.toString()))
                },
                onInvitationsClick = {
                    navController.navigate(ArticlesDestinations.InvitationsDestination)
                }
            )
        }
        composable<ArticlesDestinations.InvitationsDestination>(
            deepLinks = listOf(navDeepLink { uriPattern = INVITATIONS_DEEP_LINK })
        ) {
            InvitationsPage(onBackClick = navController::popBackStack)
        }
        composable<ArticlesDestinations.GroupDetailsDestination>(
            deepLinks = listOf(navDeepLink { uriPattern = GROUP_DETAILS_DEEP_LINK })
        ) {
            GroupDetailsPage(
                onBackClick = navController::popBackStack,
                onAddArticle = { group ->
                    navController.navigate(ArticlesDestinations.AddNewArticleDestination(groupId = group.id.toString()))
                },
                onEditArticle = { article ->
                    navController.navigate(
                        ArticlesDestinations.AddNewArticleDestination(
                            groupId = article.groupId.toString(),
                            articleId = article.id.toString()
                        )
                    )
                },
                onOpenNavigation = onOpenNavigation,
                onOpenLink = onOpenLink
            )
        }
        composable<ArticlesDestinations.AddNewArticleDestination> { backStackEntry ->
            val addNewArticleViewModel: AddNewArticleViewModel = hiltViewModel()
            val pickedLocation by backStackEntry.savedStateHandle
                .getStateFlow<UILocation?>(KEY_LOCATION, null)
                .collectAsStateWithLifecycle()

            LaunchedEffect(pickedLocation) {
                pickedLocation?.let { location ->
                    addNewArticleViewModel.handleAction(AddNewArticleAction.UpdateLocation(location))
                    backStackEntry.savedStateHandle.remove<UILocation>(KEY_LOCATION)
                }
            }

            AddNewArticle(
                onBackClick = navController::popBackStack,
                onLocationPickerClick = { location ->
                    navController.navigate(
                        ArticlesDestinations.PickLocationDestination(
                            latitude = location?.latitude,
                            longitude = location?.longitude
                        )
                    )
                },
                addNewArticleViewModel = addNewArticleViewModel
            )
        }
        composable<ArticlesDestinations.PickLocationDestination> {
            LocationPicker(
                onLocationSelected = { location ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_LOCATION, location)
                    navController.popBackStack()
                },
                onCancel = navController::popBackStack
            )
        }
        composable<ArticlesDestinations.SettingsDestination> {
            SettingsScreen(
                onSignOut = onSignOut,
                onBackClick = navController::popBackStack,
                onAccountDelete = onAccountDelete,
                onArchivedGroupsClick = {
                    navController.navigate(ArticlesDestinations.ArchivedGroupsDestination)
                }
            )
        }
        composable<ArticlesDestinations.ArchivedGroupsDestination> {
            ArchivedGroupsPage(onBackClick = navController::popBackStack)
        }
    }

}

private const val KEY_LOCATION = "KEY_LOCATION"
const val INVITATIONS_DEEP_LINK = "articles://invitations"
const val GROUP_DETAILS_DEEP_LINK = "articles://group/{groupId}"

sealed interface ArticlesDestinations {
    @Serializable data object GroupsListDestination : ArticlesDestinations
    @Serializable data class AddNewArticleDestination(val groupId: String, val articleId: String? = null) : ArticlesDestinations
    @Serializable data class PickLocationDestination(val latitude: Double? = null, val longitude: Double? = null) : ArticlesDestinations
    @Serializable data object SettingsDestination : ArticlesDestinations
    @Serializable data class GroupDetailsDestination(val groupId: String) : ArticlesDestinations
    @Serializable data object InvitationsDestination : ArticlesDestinations
    @Serializable data object ArchivedGroupsDestination : ArticlesDestinations
}