package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.example.articles.ui.composables.utils.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.Location
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.viewmodels.GroupDetailsAction
import com.example.articles.viewmodels.GroupDetailsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsPage(
    onAddArticle: (articleGroup: UIGroup) -> Unit,
    onEditArticle: (UIArticle) -> Unit,
    onBackClick: () -> Unit,
    onOpenNavigation: (Location) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    groupDetailsViewModel: GroupDetailsViewModel = hiltViewModel()
) {

    val state by groupDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    if (state.showEditDeleteBottonSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                coroutineScope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BottomSheetRowItem(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.edit_article),
                    text = stringResource(id = R.string.edit_article),
                    onClick = {
                        val articleToEdit = state.articleToEditOrDelete
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                        articleToEdit?.let { onEditArticle(it) }
                    }
                )
                BottomSheetRowItem(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.remove_article),
                    text = stringResource(id = R.string.remove_article),
                    onClick = {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.RemoveArticle)
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    }
                )
            }
        }
    }

    Scaffold { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxHeight()
                .padding(innerPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
            ) {
                Toolbar(
                    title = state.group.name,
                    leadingIcon = Icons.Default.ChevronLeft,
                    onLeadingIconClick = onBackClick
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (state.articles.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        items(state.articles) { article ->
                            ArticleItem(
                                article = article,
                                onLongClick = {
                                    groupDetailsViewModel.handleAction(
                                        GroupDetailsAction.ArticleLongClick(article = article)
                                    )
                                },
                                onOpenNavigation = onOpenNavigation,
                                onOpenLink = onOpenLink
                            )
                        }
                    }
                } else {
                    NoArticles(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(78.dp)
                            .clip(shape = RoundedCornerShape(16.dp))
                    )
                }
            }
            Button(
                onClick = {
                    onAddArticle(state.group)
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.add_new_article)
                    )
                    Text(
                        text = stringResource(id = R.string.add_new_article)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

}

@Composable
fun ArticleItem(
    article: UIArticle,
    onLongClick: (UIArticle) -> Unit,
    onOpenNavigation: (Location) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .background(color = MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(article) }
            )
    ) {
        Column {
            Box {
                Text(
                    text = article.name.toString(),
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = stringResource(id = R.string.expand)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f)
                ) {
                    if (!article.description.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = stringResource(id = R.string.article_description)
                            )
                            Text(
                                text = article.description,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (article.quantity != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = stringResource(id = R.string.quantity) + " :")
                                Text(text = stringResource(id = R.string.unit) + " :")
                                if (article.pricePerUnit != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = stringResource(id = R.string.price_per_unit) + " :")
                                }
                            }
                            Column(
                                modifier = Modifier.weight(2f)
                            ) {
                                Text(text = article.quantity.toString())
                                Text(
                                    text = article.unit.toString().first().uppercase()
                                            + article.unit.toString().drop(1).lowercase()
                                )
                                if (article.pricePerUnit != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = article.pricePerUnit.toString())
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    if (!article.websiteLink.isNullOrBlank() || article.location != null) {
                        Spacer(modifier = Modifier.height(20.dp))
                        if (!article.websiteLink.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = modifier
                                    .padding(start = 8.dp)
                                    .clickable {
                                        onOpenLink(article.websiteLink)
                                    },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = stringResource(id = R.string.website_link),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.website_link),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (article.location != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = modifier
                                    .padding(start = 8.dp)
                                    .clickable {
                                        onOpenNavigation(article.location)
                                    },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = stringResource(id = R.string.website_link),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.location),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                article.photoUri?.let { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = stringResource(id = R.string.photo),
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp, bottom = 8.dp, start = 8.dp, end = 8.dp)
                            .fillMaxSize()
                    )
                }
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