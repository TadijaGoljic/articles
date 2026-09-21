package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.articles.R
import com.example.articles.models.UIComment
import com.example.articles.models.UILocation
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.GroupMember
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.EmailAvatar
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.composables.utils.UserInfoItem
import com.example.articles.utils.ScreenSize
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 60.dp)
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

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
        ) {
            Toolbar(
                title = state.group.name,
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick
            )
            GroupDetailsModeTabs(
                mode = state.mode,
                onModeSelected = { groupDetailsViewModel.handleAction(GroupDetailsAction.SelectMode(it)) },
                commentsCount = state.groupComments.size
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
                        onAddArticle = onAddArticle,
                        onOpenNavigation = onOpenNavigation,
                        onOpenLink = onOpenLink,
                        onAction = groupDetailsViewModel::handleAction,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colorScheme.background)
                    )
                    GroupDetailsMode.COMMENTS -> GroupCommentsContent(
                        comments = state.groupComments,
                        currentUid = currentUid,
                        onAction = groupDetailsViewModel::handleAction,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color = MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }

}

private fun GroupDetailsMode.toPageIndex(): Int = if (this == GroupDetailsMode.ARTICLES) 0 else 1

private fun Int.toGroupDetailsMode(): GroupDetailsMode =
    if (this == 0) GroupDetailsMode.ARTICLES else GroupDetailsMode.COMMENTS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailsModeTabs(
    mode: GroupDetailsMode,
    onModeSelected: (GroupDetailsMode) -> Unit,
    commentsCount: Int,
    modifier: Modifier = Modifier
) {
    PrimaryTabRow(
        selectedTabIndex = if (mode == GroupDetailsMode.ARTICLES) 0 else 1,
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(
                    selectedTabIndex = if (mode == GroupDetailsMode.ARTICLES) 0 else 1,
                    matchContentSize = false
                ),
                width = Dp.Unspecified
            )
        }
    ) {
        Tab(
            selected = mode == GroupDetailsMode.ARTICLES,
            onClick = { onModeSelected(GroupDetailsMode.ARTICLES) },
            text = { Text(text = stringResource(id = R.string.articles_tab)) }
        )
        Tab(
            selected = mode == GroupDetailsMode.COMMENTS,
            onClick = { onModeSelected(GroupDetailsMode.COMMENTS) },
            text = { Text(text = stringResource(id = R.string.comments_tab) +  " (${commentsCount})") }
        )
    }
}

@Composable
private fun ArticlesModeContent(
    state: GroupDetailsState,
    screenSize: ScreenSize,
    onAddArticle: (UIGroup) -> Unit,
    onOpenNavigation: (UILocation) -> Unit,
    onOpenLink: (String) -> Unit,
    onAction: (GroupDetailsAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var orderedArticles by remember(state.articles) { mutableStateOf(state.articles) }
    val membersByUid = remember(state.groupMembers) { state.groupMembers.associateBy { it.uid } }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        orderedArticles = orderedArticles.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
    }

    Box(
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
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
                            onAction(GroupDetailsAction.ShowFilterDropdownMenu)
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
                            onAction(GroupDetailsAction.HideFilterDropdownMenu)
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(id = R.string.filters_all))
                            },
                            onClick = {
                                onAction(GroupDetailsAction.FilterSelected(FilterType.ALL))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(id = R.string.filters_bought))
                            },
                            onClick = {
                                onAction(GroupDetailsAction.FilterSelected(FilterType.BOUGHT))
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(text = stringResource(id = R.string.filters_unbought))
                            },
                            onClick = {
                                onAction(GroupDetailsAction.FilterSelected(FilterType.UNBOUGHT))
                            }
                        )
                        if (state.group.isShared) {
                            DropdownMenuItem(
                                text = {
                                    Text(text = stringResource(id = R.string.filters_assigned_to_me))
                                },
                                onClick = {
                                    onAction(GroupDetailsAction.FilterSelected(FilterType.ASSIGNED_TO_ME))
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {
                        onAction(
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
                    state = lazyListState,
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
                    items(items = orderedArticles, key = { it.id }) { article ->
                        ReorderableItem(reorderableLazyListState, key = article.id) {
                            ArticleItem(
                                article = article,
                                commentCount = state.articleCommentCounts[article.id] ?: 0,
                                assigneeEmail = article.assignedToUid?.let { membersByUid[it]?.email },
                                onLongClick = {
                                    onAction(GroupDetailsAction.ArticleLongClick(article = article))
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
                                )
                            )
                        }
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
                .padding(bottom = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.add_new_article)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.add_new_article)
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
        HorizontalDivider()
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
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        if (currentAssigneeUid != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAssign(null) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(id = R.string.unassign),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }
                        members.forEach { member ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAssign(member.uid) }
                            ) {
                                UserInfoItem(
                                    email = member.email,
                                    modifier = Modifier.weight(1f)
                                )
                                if (member.uid == currentAssigneeUid) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(text = stringResource(id = R.string.close), fontSize = 14.sp)
                }
            }
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
    modifier: Modifier = Modifier,
    commentCount: Int = 0,
    assigneeEmail: String? = null,
    onCommentsClick: () -> Unit = {},
    dragHandleModifier: Modifier = Modifier,
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = null,
                        modifier = dragHandleModifier
                            .padding(start = 4.dp)
                            .size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = article.name,
                        textAlign = TextAlign.Start,
                        fontSize = 20.sp,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                ) {
                    if (!assigneeEmail.isNullOrBlank()) {
                        EmailAvatar(
                            email = assigneeEmail,
                            size = 24.dp,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = onCommentsClick
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.height(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = commentCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onChangeExpandedStatus,
                    ) {
                        Icon(
                            imageVector =
                                if (!article.expanded) Icons.Default.KeyboardArrowDown
                                else Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(id = R.string.expand)
                        )
                    }
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
