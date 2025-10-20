package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.sync.SyncManager
import com.example.articles.models.UIArticle
import com.example.articles.ui.utils.DateUnit
import com.example.articles.ui.utils.Toolbar
import com.example.articles.ui.utils.isInteger
import com.example.articles.ui.utils.toUIDate
import com.example.articles.viewmodels.ArticlesListViewModel
import com.example.articles.viewmodels.LocationData
import kotlinx.coroutines.delay

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticlesList(
    onBackClick: () -> Unit,
    onFloatingActionButtonClick: () -> Unit,
    onOpenNavigation: (LocationData) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    articlesListViewModel: ArticlesListViewModel = hiltViewModel()
) {

    val state by articlesListViewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler {
        if (state.shoppingModeActive) articlesListViewModel.stopShopping(false)
        else onBackClick()
    }

    LaunchedEffect(Unit) {
        articlesListViewModel.syncData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onFloatingActionButtonClick()
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_new_article)
                )
            }
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
                    if (state.shoppingModeActive) articlesListViewModel.stopShopping(true)
                    else articlesListViewModel.startShopping(null)
                },
                leadingIcon = Icons.Default.Settings,
                color = MaterialTheme.colorScheme.onBackground,
                onLeadingIconClick = onOpenSettings
            )
            LazyColumn {
                itemsIndexed(state.articles) { index, article ->
                    ArticleItem(
                        article = article,
                        onOpenNavigation = onOpenNavigation,
                        shoppingModeRunning = state.shoppingModeActive,
                        onCheckChange = {
                            articlesListViewModel.updateCheckStatus(index, it)
                        },
                        togglePhoto = {
                            articlesListViewModel.togglePhoto(index)
                        },
                        onItemBought = {
                            articlesListViewModel.buy(article)
                        },
                        onShoppingModeClick = {
                            articlesListViewModel.startShopping(index)
                        },
                        onEditClick = {

                        },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
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
    onOpenNavigation: (LocationData) -> Unit,
    onCheckChange: (Boolean) -> Unit,
    onItemBought: () -> Unit,
    shoppingModeRunning: Boolean,
    onShoppingModeClick: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val uiDate = article.dateCreated.toUIDate()
    val dateText =
        uiDate.value.toString() + " " +
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
        } + " " + stringResource(id = R.string.ago)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onDoubleClick = onItemBought,
                onLongClick = {
                    onShoppingModeClick(true)
                },
                onClick = {
                    if (shoppingModeRunning) onCheckChange(!article.checked)
                    else onEditClick()
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
                article.name?.let { name ->
                    Text(
                        text = name,
                        fontSize = 24.sp
                    )
                }
                if (article.quantity != null) {
                    Text(
                        text = ": " +
                                if (article.quantity.isInteger()) article.quantity.toInt()
                                    .toString()
                                else article.quantity.toString(),
                        fontSize = 24.sp
                    )
                    Text(
                        text = stringResource(id = article.unit.abbreviation),
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
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
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = dateText
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