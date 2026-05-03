package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.utils.DateUnit
import com.example.articles.ui.utils.ScreenSize
import com.example.articles.ui.utils.isInteger
import com.example.articles.ui.utils.toUIDate
import com.example.articles.viewmodels.ArticlesListViewModel
import com.example.articles.models.Location
import androidx.compose.ui.window.Dialog
import com.example.articles.ui.composables.utils.TwoOptionFab
import com.example.articles.viewmodels.ArticlesListAction

@Composable
fun ArticlesList(
    onBackClick: () -> Unit,
    openAddNewArticleScreen: () -> Unit,
    openCreateGroupScreen: () -> Unit,
    onOpenNavigation: (Location) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    articlesListViewModel: ArticlesListViewModel = hiltViewModel()
) {

    val state by articlesListViewModel.uiState.collectAsStateWithLifecycle()
    val screenSize = ScreenSize.getScreenSize()

    BackHandler {
        if (state.shoppingModeActive) {
            articlesListViewModel.handleAction(ArticlesListAction.StopShopping(false))
        } else {
            onBackClick()
        }
    }

    LaunchedEffect(Unit) {
        articlesListViewModel.handleAction(ArticlesListAction.SyncData)
    }

    Scaffold(
        floatingActionButton = {
            TwoOptionFab(
                firstIcon = Icons.Default.Add,
                firstText = stringResource(id = R.string.create_group),
                onFirstClick = openCreateGroupScreen,
                secondIcon = Icons.Default.Add,
                secondText = stringResource(id = R.string.add_new_article),
                onSecondClick = openAddNewArticleScreen,
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = MaterialTheme.colorScheme.background)
        ) {
            Toolbar(
                title = stringResource(id = R.string.articles_list),
                trailingIcon =
                    if (state.shoppingModeActive) Icons.Default.Delete
                    else Icons.Default.ShoppingCart,
                onTrailingIconClick = {
                    if (state.shoppingModeActive) articlesListViewModel.handleAction(ArticlesListAction.StopShopping(deleteArticles = true))
                    else articlesListViewModel.handleAction(ArticlesListAction.StartShopping(index = null))
                },
                trailingIconEnabled = state.articles.isNotEmpty(),
                leadingIcon = Icons.Default.Settings,
                onLeadingIconClick = onOpenSettings,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (state.articles.isEmpty()) {
                NoArticles(
                    modifier = when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                )
            } else {
                LazyColumn(
                    modifier = when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                ) {
                    itemsIndexed(
                        items = state.articles,
                        key = { _, article -> article.id }
                    ) { index, article ->
                        ArticleItem(
                            article = article,
                            onOpenNavigation = onOpenNavigation,
                            shoppingModeRunning = state.shoppingModeActive,
                            onCheckChange = {
                                articlesListViewModel.handleAction(ArticlesListAction.ToggleCheckStatus(index, it))
                            },
                            togglePhoto = {
                                articlesListViewModel.handleAction(ArticlesListAction.TogglePhoto(index))
                            },
                            onItemBought = {
                                articlesListViewModel.handleAction(ArticlesListAction.BuyArticle(article))
                            },
                            onShoppingModeClick = {
                                articlesListViewModel.handleAction(ArticlesListAction.StartShopping(index = index))
                            },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

}

@SuppressLint("LocalContextResourcesRead")
@Composable
fun ArticleItem(
    article: UIArticle,
    togglePhoto: () -> Unit,
    onOpenNavigation: (Location) -> Unit,
    onCheckChange: (Boolean) -> Unit,
    onItemBought: () -> Unit,
    shoppingModeRunning: Boolean,
    onShoppingModeClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val uiDate = article.dateCreated.toUIDate()
    val dateText = buildString {
        append(uiDate.value.toString())
        append(" ")
        append(
            when (uiDate.unit) {
                DateUnit.DAY -> context.resources.getQuantityString(
                        R.plurals.days,
                        uiDate.value
                    )
                DateUnit.HOUR -> context.resources.getQuantityString(
                        R.plurals.hours,
                        uiDate.value
                    )
                DateUnit.MINUTE -> context.resources.getQuantityString(
                        R.plurals.minutes,
                        uiDate.value
                    )
                DateUnit.SECOND -> context.resources.getQuantityString(
                        R.plurals.seconds,
                        uiDate.value
                    )
            }
        )
        append(" ")
        append(stringResource(id = R.string.ago))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onDoubleClick = {
                    if (!shoppingModeRunning) onItemBought()
                },
                onLongClick = {
                    onShoppingModeClick(true)
                },
                onClick = {
                    if (shoppingModeRunning) onCheckChange(!article.checked)
                }
            )
            .padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = togglePhoto,
                    modifier = Modifier.size(24.dp),
                    enabled = article.photoUri != null
                ) {
                    Icon(
                        imageVector =
                            if (article.photoVisible) Icons.Default.KeyboardArrowDown
                            else Icons.Default.ChevronRight,
                        contentDescription =
                            if (article.photoVisible) stringResource(id = R.string.hide_photo)
                            else stringResource(id = R.string.show_photo),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                val text = buildString {
                    article.name?.let { name ->
                        append(name)
                    }
                    if (article.quantity != null) {
                        append(": ")
                        append(
                            if (article.quantity.isInteger()) article.quantity.toInt().toString()
                            else article.quantity.toString()
                        )
                        append(" ")
                        append(stringResource(id = article.unit.abbreviation))
                    }
                }
                Text(
                    text = text,
                    fontSize = 24.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f),
                    maxLines = 1
                )
                IconButton(
                    onClick = {
                        onOpenNavigation(article.location ?: return@IconButton)
                    },
                    modifier = Modifier.size(24.dp),
                    enabled = article.location != null,
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = stringResource(id = R.string.start_navigation)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateText,
                    maxLines = 1,
                    modifier = Modifier.wrapContentWidth()
                )
                AnimatedVisibility(visible = shoppingModeRunning) {
                    Row {
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = article.checked,
                            onCheckedChange = onCheckChange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = article.photoVisible && article.photoUri != null,
            ) {
                AsyncImage(
                    model = article.photoUri,
                    contentDescription = article.name,
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                )
            }
        }
    }

}

@Composable
fun NoArticles(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(15.dp)
            .height(78.dp)
            .clip(shape = RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_articles_title),
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.no_articles_message),
                color = Color.Gray,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}