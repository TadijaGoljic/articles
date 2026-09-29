package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIComment
import com.example.articles.models.UIGroup
import com.example.articles.models.UILocation
import com.example.articles.repositories.GroupMember
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.EmailAvatar
import com.example.articles.ui.composables.utils.EmptyState
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.composables.utils.UserInfoItem
import com.example.articles.utils.ScreenSize
import com.example.articles.utils.tryToInt
import com.example.articles.viewmodels.FilterType
import com.example.articles.viewmodels.GroupDetailsAction
import com.example.articles.viewmodels.GroupDetailsMode
import com.example.articles.viewmodels.GroupDetailsState
import com.example.articles.viewmodels.GroupDetailsViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Locale

@SuppressLint("CoroutineCreationDuringComposition")
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
    val currentUid = Firebase.auth.currentUser?.uid

    BackHandler(enabled = state.mode == GroupDetailsMode.COMMENTS) {
        groupDetailsViewModel.handleAction(GroupDetailsAction.SelectMode(GroupDetailsMode.ARTICLES))
    }

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
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                state.articleToEditOrDelete?.let { article ->
                    Text(
                        text = article.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                    )
                }
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
                if (state.group.isShared) {
                    BottomSheetRowItem(
                        icon = Icons.Default.Person,
                        contentDescription = stringResource(id = R.string.assign_article),
                        text = stringResource(id = R.string.assign_article),
                        onClick = {
                            groupDetailsViewModel.handleAction(GroupDetailsAction.ShowAssignDialog)
                        }
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
                BottomSheetRowItem(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(id = R.string.remove_article),
                    text = stringResource(id = R.string.remove_article),
                    onClick = {
                        groupDetailsViewModel.handleAction(GroupDetailsAction.RemoveArticle)
                        groupDetailsViewModel.handleAction(GroupDetailsAction.HideBottomSheet)
                    },
                    isDestructive = true
                )
            }
        }
    }

    if (state.showCommentsSheet) {
        val commentsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val commentsCoroutineScope = rememberCoroutineScope()
        CommentsBottomSheet(
            comments = state.articleComments,
            currentUid = currentUid,
            sheetState = commentsSheetState,
            subtitle = state.articleCommentsTarget?.name,
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

    if (state.showAssignDialog) {
        AssignArticleDialog(
            members = state.groupMembers,
            isLoading = state.isLoadingGroupMembers,
            currentAssigneeUid = state.articleToEditOrDelete?.assignedToUid,
            onAssign = { uid ->
                groupDetailsViewModel.handleAction(GroupDetailsAction.AssignArticle(assignedToUid = uid))
            },
            onDismiss = {
                groupDetailsViewModel.handleAction(GroupDetailsAction.HideAssignDialog)
            }
        )
    }

    val boughtCount = state.articles.count { it.bought }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.mode == GroupDetailsMode.ARTICLES,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { onAddArticle(state.group) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                    },
                    text = { Text(text = stringResource(id = R.string.add_new_article)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Toolbar(
                title = state.group.name,
                subtitle =
                    if (state.selectedFilter == FilterType.ALL && state.articles.isNotEmpty())
                        stringResource(id = R.string.bought_progress, boughtCount, state.articles.size)
                    else null,
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick
            )
            GroupDetailsModeTabs(
                mode = state.mode,
                onModeSelected = { groupDetailsViewModel.handleAction(GroupDetailsAction.SelectMode(it)) },
                commentsCount = state.groupComments.size,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            val pagerState = rememberPagerState(
                initialPage = state.mode.toPageIndex(),
                pageCount = { GroupDetailsMode.entries.size }
            )
            LaunchedEffect(state.mode) {
                val targetPage = state.mode.toPageIndex()
                if (pagerState.currentPage != targetPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
            LaunchedEffect(pagerState.currentPage) {
                val swipedMode = pagerState.currentPage.toGroupDetailsMode()
                if (swipedMode != state.mode) {
                    groupDetailsViewModel.handleAction(GroupDetailsAction.SelectMode(swipedMode))
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page.toGroupDetailsMode()) {
                    GroupDetailsMode.ARTICLES -> ArticlesModeContent(
                        state = state,
                        screenSize = screenSize,
                        onOpenNavigation = onOpenNavigation,
                        onOpenLink = onOpenLink,
                        onAction = groupDetailsViewModel::handleAction,
                        modifier = Modifier.fillMaxSize()
                    )
                    GroupDetailsMode.COMMENTS -> GroupCommentsContent(
                        comments = state.groupComments,
                        currentUid = currentUid,
                        onAction = groupDetailsViewModel::handleAction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

}

private fun GroupDetailsMode.toPageIndex(): Int = if (this == GroupDetailsMode.ARTICLES) 0 else 1

private fun Int.toGroupDetailsMode(): GroupDetailsMode =
    if (this == 0) GroupDetailsMode.ARTICLES else GroupDetailsMode.COMMENTS

@Composable
private fun GroupDetailsModeTabs(
    mode: GroupDetailsMode,
    onModeSelected: (GroupDetailsMode) -> Unit,
    commentsCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
    ) {
        SegmentTab(
            text = stringResource(id = R.string.articles_tab),
            selected = mode == GroupDetailsMode.ARTICLES,
            onClick = { onModeSelected(GroupDetailsMode.ARTICLES) },
            modifier = Modifier.weight(1f)
        )
        SegmentTab(
            text = stringResource(id = R.string.comments_tab) + " (${commentsCount})",
            selected = mode == GroupDetailsMode.COMMENTS,
            onClick = { onModeSelected(GroupDetailsMode.COMMENTS) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer,
        label = "segmentBackground"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (selected) Modifier.shadow(2.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ArticlesModeContent(
    state: GroupDetailsState,
    screenSize: ScreenSize,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    onAction: (GroupDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var orderedArticles by remember(state.articles) { mutableStateOf(state.articles) }
    val membersByUid = remember(state.groupMembers) { state.groupMembers.associateBy { it.uid } }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = orderedArticles.indexOfFirst { it.id == from.key }
        val toIndex = orderedArticles.indexOfFirst { it.id == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            orderedArticles = orderedArticles.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 104.dp),
        modifier = modifier
            .then(
                when (screenSize) {
                    ScreenSize.NORMAL -> Modifier
                    ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                    ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                }
            )
    ) {
        if (state.selectedFilter == FilterType.ALL && state.articles.isNotEmpty()) {
            item(key = "summary") {
                ShoppingSummaryCard(
                    articles = state.articles,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
        item(key = "filters") {
            FiltersRow(
                selectedFilter = state.selectedFilter,
                showAssignedToMe = state.group.isShared,
                showExpandToggle = state.articles.any { it.hasDetails() },
                allExpanded = state.articles.isNotEmpty() && state.articles.all { it.expanded },
                onFilterSelected = { onAction(GroupDetailsAction.FilterSelected(it)) },
                onToggleExpandAll = { allExpanded ->
                    onAction(
                        if (allExpanded) GroupDetailsAction.CollapseAllArticles
                        else GroupDetailsAction.ExpandAllArticles
                    )
                }
            )
        }
        if (orderedArticles.isNotEmpty()) {
            items(items = orderedArticles, key = { it.id }) { article ->
                ReorderableItem(reorderableLazyListState, key = article.id) {
                    ArticleItem(
                        article = article,
                        commentCount = state.articleCommentCounts[article.id] ?: 0,
                        assigneeEmail = article.assignedToUid?.let { membersByUid[it]?.email },
                        onLongClick = {
                            onAction(GroupDetailsAction.ArticleLongClick(article = article))
                        },
                        onToggleBought = {
                            onAction(GroupDetailsAction.ToggleBought(article = article))
                        },
                        onCommentsClick = {
                            onAction(GroupDetailsAction.ShowArticleComments(article = article))
                        },
                        onOpenNavigation = onOpenNavigation,
                        onOpenLink = onOpenLink,
                        onChangeExpandedStatus = {
                            onAction(
                                GroupDetailsAction.UpdateExpandedStatus(
                                    article = article,
                                    expanded = !article.expanded
                                )
                            )
                        },
                        dragHandleModifier = Modifier.draggableHandle(
                            onDragStopped = {
                                val index = orderedArticles.indexOfFirst { it.id == article.id }
                                if (index != -1) {
                                    onAction(
                                        GroupDetailsAction.ReorderArticle(
                                            articleId = article.id,
                                            previousPosition = orderedArticles.getOrNull(index - 1)?.position,
                                            nextPosition = orderedArticles.getOrNull(index + 1)?.position
                                        )
                                    )
                                }
                            }
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            item(key = "empty") {
                NoArticles(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShoppingSummaryCard(
    articles: List<UIArticle>,
    modifier: Modifier = Modifier
) {
    val boughtCount = articles.count { it.bought }
    val hasPrices = articles.any { it.price != null }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = boughtCount.toString(),
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = " / ${articles.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Text(
                        text =
                            if (boughtCount == articles.size) stringResource(id = R.string.all_bought)
                            else stringResource(id = R.string.filters_bought),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (hasPrices) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = articles.sumOf { it.price ?: 0.0 }.tryToInt().toString(),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(id = R.string.total),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { boughtCount / articles.size.toFloat() },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersRow(
    selectedFilter: FilterType,
    showAssignedToMe: Boolean,
    showExpandToggle: Boolean,
    allExpanded: Boolean,
    onFilterSelected: (FilterType) -> Unit,
    onToggleExpandAll: (allExpanded: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = buildList {
        add(FilterType.ALL to R.string.filters_all)
        add(FilterType.UNBOUGHT to R.string.filters_unbought)
        add(FilterType.BOUGHT to R.string.filters_bought)
        if (showAssignedToMe) add(FilterType.ASSIGNED_TO_ME to R.string.filters_assigned_to_me)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 8.dp)
        ) {
            filters.forEach { (filter, label) ->
                val selected = filter == selectedFilter
                FilterChip(
                    selected = selected,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(text = stringResource(id = label)) },
                    leadingIcon =
                        if (selected) {
                            { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
        if (showExpandToggle) {
            IconButton(
                onClick = { onToggleExpandAll(allExpanded) },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                    contentDescription =
                        if (allExpanded) stringResource(id = R.string.collapse_all_articles)
                        else stringResource(id = R.string.expand_all_articles),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupCommentsContent(
    comments: List<UIComment>,
    currentUid: String?,
    onAction: (GroupDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var commentText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f)) {
            CommentsList(
                comments = comments,
                currentUid = currentUid,
                onDeleteComment = { comment -> onAction(GroupDetailsAction.DeleteComment(comment)) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        CommentComposer(
            value = commentText,
            onValueChange = { commentText = it },
            onSend = {
                if (commentText.isNotBlank()) {
                    onAction(GroupDetailsAction.AddComment(commentText))
                    commentText = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(color = MaterialTheme.colorScheme.surface)
                .imePadding()
                .padding(16.dp)
        )
    }
}

@Composable
private fun AssignArticleDialog(
    members: List<GroupMember>,
    isLoading: Boolean,
    currentAssigneeUid: String?,
    onAssign: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier
        ) {
            Column(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Toolbar(title = stringResource(id = R.string.assign_article_title))
                if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        if (currentAssigneeUid != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onAssign(null) }
                                    .padding(horizontal = 12.dp, vertical = 14.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.unassign),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        members.forEach { member ->
                            val isAssignee = member.uid == currentAssigneeUid
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(
                                        if (isAssignee) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { onAssign(member.uid) }
                                    .padding(horizontal = 12.dp)
                            ) {
                                UserInfoItem(
                                    email = member.email,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isAssignee) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 8.dp, end = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.close))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleItem(
    article: UIArticle,
    onLongClick: (UIArticle) -> Unit,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    onChangeExpandedStatus: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleBought: () -> Unit = {},
    commentCount: Int = 0,
    assigneeEmail: String? = null,
    onCommentsClick: () -> Unit = {},
    dragHandleModifier: Modifier = Modifier,
) {

    val hasDetails = article.hasDetails()
    val showDetails = hasDetails && article.expanded
    val unitAbbreviation = stringResource(id = article.unit.abbreviation)
    val metaLine = listOfNotNull(
        article.quantity?.let { "${it.tryToInt()} $unitAbbreviation" },
        article.price?.let { "${stringResource(id = R.string.price)} ${it.tryToInt()}" }
    ).joinToString(separator = "  ·  ")

    Surface(
        shape = MaterialTheme.shapes.large,
        color =
            if (article.bought) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            onClick = { if (hasDetails) onChangeExpandedStatus() },
                            onLongClick = { onLongClick(article) }
                        )
                        .heightIn(min = 64.dp)
                        .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    IconButton(onClick = onToggleBought) {
                        Icon(
                            imageVector =
                                if (article.bought) Icons.Default.CheckCircle
                                else Icons.Default.RadioButtonUnchecked,
                            contentDescription = stringResource(
                                id = if (article.bought) R.string.mark_as_not_bought else R.string.mark_as_bought
                            ),
                            tint =
                                if (article.bought) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 2.dp)
                    ) {
                        Text(
                            text = article.name,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (article.bought) TextDecoration.LineThrough else null,
                            color =
                                if (article.bought) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (metaLine.isNotEmpty()) {
                            Text(
                                text = metaLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (article.photoUri != null && !showDetails) {
                        AsyncImage(
                            model = article.photoUri,
                            contentDescription = stringResource(id = R.string.photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                        )
                    }
                    if (!assigneeEmail.isNullOrBlank()) {
                        EmailAvatar(
                            email = assigneeEmail,
                            size = 26.dp,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    CommentPill(
                        count = commentCount,
                        onClick = onCommentsClick,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    if (hasDetails) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription =
                                if (article.expanded) stringResource(id = R.string.collapse)
                                else stringResource(id = R.string.expand),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .rotate(if (article.expanded) 180f else 0f)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    modifier = dragHandleModifier
                        .padding(end = 10.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 54.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    if (!article.description.isNullOrBlank()) {
                        Text(
                            text = article.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val hasChips = !article.websiteLink.isNullOrBlank() || article.location != null || article.dateTo != null
                    if (hasChips) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!article.websiteLink.isNullOrBlank()) {
                                DetailChip(
                                    icon = Icons.Default.Language,
                                    text = stringResource(id = R.string.website_link),
                                    onClick = { onOpenLink(article.websiteLink) }
                                )
                            }
                            if (article.location != null) {
                                DetailChip(
                                    icon = Icons.Default.LocationOn,
                                    text = stringResource(id = R.string.location),
                                    onClick = { onOpenNavigation(article.location) }
                                )
                            }
                            article.dateTo?.let { dateTo ->
                                val formatted = remember(dateTo) {
                                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(dateTo)
                                }
                                DetailChip(
                                    icon = Icons.Default.Event,
                                    text = formatted,
                                    onClick = {}
                                )
                            }
                        }
                    }
                    article.photoUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = stringResource(id = R.string.photo),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = count > 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Comment,
            contentDescription = stringResource(id = R.string.comments),
            tint =
                if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (active) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DetailChip(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = text) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(AssistChipDefaults.IconSize)
            )
        },
        shape = CircleShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            leadingIconContentColor = MaterialTheme.colorScheme.primary
        ),
        border = null
    )
}

@Composable
fun NoArticles(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.ShoppingBasket,
        title = stringResource(id = R.string.no_articles_title),
        message = stringResource(id = R.string.no_articles_message),
        modifier = modifier
    )
}

/** Whether the article has anything to show below its title row. */
private fun UIArticle.hasDetails(): Boolean =
    !description.isNullOrBlank() ||
        !websiteLink.isNullOrBlank() ||
        location != null ||
        dateTo != null ||
        photoUri != null
