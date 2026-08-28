package com.example.articles.ui.composables.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.articles.models.UILocation
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.FilterType
import com.example.articles.viewmodels.GroupDetailsAction
import com.example.articles.viewmodels.GroupDetailsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailsPage(
    onAddArticle: (articleGroup: UIGroup) -> Unit,
    onEditArticle: (UIArticle) -> Unit,
    onBackClick: () -> Unit,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    groupDetailsViewModel: GroupDetailsViewModel = hiltViewModel()
) {

    val state by groupDetailsViewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val screenSize = ScreenSize.getScreenSize()

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
                if (state.articleToEditOrDelete?.bought == true) {
                    BottomSheetRowItem(
                        icon = Icons.Default.RemoveShoppingCart,
                        contentDescription = stringResource(id = R.string.mark_as_not_bought),
                        text = stringResource(id = R.string.mark_as_not_bought),
                        onClick = {
                            groupDetailsViewModel.handleAction(GroupDetailsAction.MarkAsNotBought)
                            groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                        }
                    )
                } else {
                    BottomSheetRowItem(
                        icon = Icons.Default.ShoppingCart,
                        contentDescription = stringResource(id = R.string.mark_as_bought),
                        text = stringResource(id = R.string.mark_as_bought),
                        onClick = {
                            groupDetailsViewModel.handleAction(GroupDetailsAction.MarkAsBought)
                            groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                        }
                    )
                }
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
                BottomSheetRowItem(
                    icon = Icons.AutoMirrored.Filled.Comment,
                    contentDescription = stringResource(id = R.string.comments),
                    text = stringResource(id = R.string.comments),
                    onClick = {
                        val article = state.articleToEditOrDelete
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                        article?.let {
                            groupDetailsViewModel.handleAction(GroupDetailsAction.ShowArticleComments(it))
                        }
                    }
                )
            }
        }
    }

    if (state.showCommentsSheet) {
        val commentsSheetState = rememberModalBottomSheetState()
        val commentsCoroutineScope = rememberCoroutineScope()
        CommentsBottomSheet(
            comments = state.comments,
            currentUid = Firebase.auth.currentUser?.uid,
            sheetState = commentsSheetState,
            onDismiss = {
                commentsCoroutineScope
                    .launch { commentsSheetState.hide() }
                    .invokeOnCompletion {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideCommentsSheet)
                    }
            },
            onAddComment = { text ->
                groupDetailsViewModel.handleAction(GroupDetailsAction.AddComment(text))
            },
            onDeleteComment = { comment ->
                groupDetailsViewModel.handleAction(GroupDetailsAction.DeleteComment(comment))
            }
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                ) {
                    Box {
                        Button(
                            onClick = {
                                groupDetailsViewModel.handleAction(GroupDetailsAction.ShowFilterDropdownMenu)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .width(50.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.filter),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = stringResource(id = R.string.filter),
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = state.filterDropdownMenuVisible,
                            onDismissRequest = {
                                groupDetailsViewModel.handleAction(GroupDetailsAction.HideFilterDropdownMenu)
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.filters_all))
                                },
                                onClick = {
                                    groupDetailsViewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.ALL))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.filters_bought))
                                },
                                onClick = {
                                    groupDetailsViewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.BOUGHT))
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.filters_unbought))
                                },
                                onClick = {
                                    groupDetailsViewModel.handleAction(GroupDetailsAction.FilterSelected(FilterType.UNBOUGHT))
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            groupDetailsViewModel.handleAction(GroupDetailsAction.ShowGroupComments)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Comment,
                            contentDescription = stringResource(id = R.string.comments)
                        )
                    }
                    TextButton(
                        onClick = {
                            groupDetailsViewModel.handleAction(
                                if (state.articles.all { it.expanded }) GroupDetailsAction.CollapseAllArticles
                                else GroupDetailsAction.ExpandAllArticles
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onBackground,
                        )
                    ) {
                        Text(
                            text =
                                if (state.articles.all { it.expanded }) stringResource(id = R.string.collapse_all_articles)
                                else stringResource(id = R.string.expand_all_articles)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (state.articles.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(bottom = 60.dp)
                            .then(
                                when (screenSize) {
                                    ScreenSize.NORMAL -> Modifier
                                    ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                                    ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                                }
                            )
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
                                onOpenLink = onOpenLink,
                                onChangeExpandedStatus = {
                                    groupDetailsViewModel.handleAction(
                                        GroupDetailsAction.UpdateExpandedStatus(
                                            article = article,
                                            expanded = !article.expanded
                                        )
                                    )
                                }
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
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
fun ArticleItem(
    article: UIArticle,
    onLongClick: (UIArticle) -> Unit,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    onChangeExpandedStatus: () -> Unit,
    modifier: Modifier = Modifier
) {

    val hasDescription = !article.description.isNullOrBlank()
    val hasQuantitySection = article.quantity != null
    val hasLocationWebsiteSection = !article.websiteLink.isNullOrBlank() || article.location != null

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(shape = RoundedCornerShape(10.dp))
            .background(
                color =
                    if (article.bought) Color.Green.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(article) }
            )
    ) {
        Column {
            Box {
                Text(
                    text = article.name,
                    textAlign = TextAlign.Start,
                    fontSize = 20.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = onChangeExpandedStatus,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector =
                            if (!article.expanded) Icons.Default.KeyboardArrowDown
                            else Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(id = R.string.expand)
                    )
                }
            }
            AnimatedVisibility(
                visible = article.expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(2f)
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
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (hasQuantitySection) {
                                if (hasDescription) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
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
                                        if (article.price != null) {
                                            Text(text = stringResource(id = R.string.price) + " :")
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
                                        if (article.price != null) {
                                            Text(text = article.price.toString())
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            if (hasLocationWebsiteSection) {
                                if (hasQuantitySection || hasDescription) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
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