package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ShoppingBasket
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.repositories.GroupMember
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.EmailAvatar
import com.example.articles.ui.composables.utils.EmptyState
import com.example.articles.ui.composables.utils.InitialTile
import com.example.articles.ui.composables.utils.OneTextFieldDialog
import com.example.articles.ui.composables.utils.RoundIconButton
import com.example.articles.ui.composables.utils.groupAccentColor
import com.example.articles.utils.ScreenSize
import com.example.articles.utils.tryToInt
import com.example.articles.viewmodels.GroupsPageAction
import com.example.articles.viewmodels.GroupsPageViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListPage(
    onGroupClick: (UIGroup) -> Unit,
    onSettingsClick: () -> Unit,
    onInvitationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    groupsPageViewModel: GroupsPageViewModel = hiltViewModel()
) {

    val state by groupsPageViewModel.uiState.collectAsStateWithLifecycle()
    val groupNames by remember {
        derivedStateOf {
            state.groups.map { group -> group.name.trim().uppercase() }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val screenSize = ScreenSize.getScreenSize()
    val context = LocalContext.current

    var orderedGroups by remember(state.groups) { mutableStateOf(state.groups) }
    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromIndex = orderedGroups.indexOfFirst { it.id == from.key }
        val toIndex = orderedGroups.indexOfFirst { it.id == to.key }
        if (fromIndex != -1 && toIndex != -1) {
            orderedGroups = orderedGroups.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
        }
    }

    LaunchedEffect(state.invitationSentEvent) {
        if (state.invitationSentEvent) {
            Toast.makeText(context, context.getString(R.string.invitation_sent), Toast.LENGTH_SHORT).show()
            groupsPageViewModel.handleAction(GroupsPageAction.ConsumeInvitationSentEvent)
        }
    }

    LaunchedEffect(state.archivedGroupEvent) {
        state.archivedGroupEvent?.let { groupName ->
            Toast.makeText(context, context.getString(R.string.group_archived, groupName), Toast.LENGTH_SHORT).show()
            groupsPageViewModel.handleAction(GroupsPageAction.ConsumeArchivedGroupEvent)
        }
    }

    if (state.showAddNewGroupDialog) {
        OneTextFieldDialog(
            title = stringResource(id = R.string.create_group),
            onDismissRequest = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideAddNewGroupDialog)
            },
            textFieldState = state.groupToCreateName,
            isError = state.isGroupCreationError,
            errorMessage =
                if (state.groupCreationErrorResource != null) stringResource(id = state.groupCreationErrorResource!!)
                else "",
            dismissButtonText = stringResource(id = R.string.cancel),
            confirmButtonText = stringResource(id = R.string.save),
            confirmButtonEnabled = state.groupToCreateName.text.isNotBlank(),
            onConfirmButtonClick = {
                groupsPageViewModel.handleAction(GroupsPageAction.SaveGroup(state.groupToCreateName.text.toString()))
            },
            isConfirmButtonLoading = state.isCreateGroupDialogLoading,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showShareGroupDialog) {
        ShareGroupDialog(
            emailState = state.shareGroupEmail,
            isLoading = state.isShareGroupLoading,
            errorMessage =
                if (state.groupSharingErrorResource != null) stringResource(id = state.groupSharingErrorResource!!)
                else null,
            recentRecipients = state.recentShareRecipients,
            onRecentRecipientClick = { email ->
                groupsPageViewModel.handleAction(GroupsPageAction.SelectRecentShareRecipient(email))
            },
            onDismiss = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideShareGroupDialog)
            },
            onConfirm = {
                groupsPageViewModel.handleAction(GroupsPageAction.ShareGroup(state.shareGroupEmail.text.toString()))
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showGroupMembersDialog) {
        val longPressedSharedGroupId = state.longPressedGroup?.sharedGroupId
        GroupMembersDialog(
            members = state.groupMembers[longPressedSharedGroupId].orEmpty(),
            isLoading = longPressedSharedGroupId in state.loadingGroupMembersFor,
            currentUid = state.currentUid,
            onRemoveMember = { member ->
                groupsPageViewModel.handleAction(GroupsPageAction.RemoveGroupMember(member))
            },
            onDismiss = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideGroupMembersDialog)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showEditGroupDialog) {
        val editNameUpper = state.groupToEditName.text.toString().trim().uppercase()
        val originalNameUpper = state.longPressedGroup?.name?.trim()?.uppercase()
        val isDuplicateInEdit = editNameUpper in groupNames && editNameUpper != originalNameUpper
        OneTextFieldDialog(
            title = stringResource(id = R.string.edit_group),
            onDismissRequest = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideEditGroupDialog)
            },
            textFieldState = state.groupToEditName,
            isError = isDuplicateInEdit,
            errorMessage = stringResource(id = R.string.group_already_exists),
            dismissButtonText = stringResource(id = R.string.cancel),
            confirmButtonText = stringResource(id = R.string.save),
            confirmButtonEnabled = state.groupToEditName.text.isNotBlank(),
            onConfirmButtonClick = {
                groupsPageViewModel.handleAction(GroupsPageAction.EditGroup(state.longPressedGroup ?: UIGroup(), state.groupToEditName.text.toString()))
            },
            isConfirmButtonLoading = state.isEditDialogLoading,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showAiAgentDialog) {
        AiAgentDialog(
            promptState = state.agentPromptText,
            isProcessing = state.isAgentProcessing,
            errorMessage = state.agentError,
            pendingCommand = state.agentPendingCommand,
            onDismissRequest = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideAiAgentDialog)
            },
            onSubmit = { text ->
                groupsPageViewModel.handleAction(GroupsPageAction.SubmitAgentPrompt(text))
            },
            onConfirmPending = {
                groupsPageViewModel.handleAction(GroupsPageAction.ConfirmPendingAgentCommand)
            },
            onCancelPending = {
                groupsPageViewModel.handleAction(GroupsPageAction.CancelPendingAgentCommand)
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    groupsPageViewModel.handleAction(GroupsPageAction.ShowAddNewGroupDialog)
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.create_group)
                    )
                },
                text = { Text(text = stringResource(id = R.string.new_group)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.large
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .then(
                    when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                )
        ) {
            item(key = "header") {
                GroupsHeader(
                    groups = state.groups,
                    pendingInvitationsCount = state.pendingInvitationsCount,
                    onInvitationsClick = onInvitationsClick,
                    onSettingsClick = onSettingsClick,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item(key = "assistant") {
                AssistantCard(
                    onClick = { groupsPageViewModel.handleAction(GroupsPageAction.ShowAiAgentDialog) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            if (orderedGroups.isNotEmpty()) {
                items(items = orderedGroups, key = { it.id }) { group ->
                    ReorderableItem(reorderableLazyListState, key = group.id) {
                        GroupItem(
                            group = group,
                            members = state.groupMembers[group.sharedGroupId].orEmpty(),
                            currentUid = state.currentUid,
                            onLongClick = {
                                groupsPageViewModel.handleAction(GroupsPageAction.LongClick(group))
                            },
                            onClick = {
                                onGroupClick(group)
                            },
                            onIconClick = {
                                groupsPageViewModel.handleAction(
                                    if (!group.expanded) GroupsPageAction.ExpandGroup(group)
                                    else GroupsPageAction.CollapseGroup(group)
                                )
                            },
                            dragHandleModifier = Modifier.draggableHandle(
                                onDragStopped = {
                                    groupsPageViewModel.handleAction(
                                        GroupsPageAction.ReorderGroup(
                                            groupId = group.id,
                                            orderedGroupIds = orderedGroups.map { it.id }
                                        )
                                    )
                                }
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                item(key = "empty") {
                    NoGroups(modifier = Modifier.fillMaxWidth())
                }
            }
        }
        if (state.showEditDeleteBottonSheet) {
            val hideSheetThen: (GroupsPageAction) -> Unit = { action ->
                coroutineScope
                    .launch { sheetState.hide() }
                    .invokeOnCompletion { groupsPageViewModel.handleAction(action) }
            }
            ModalBottomSheet(
                onDismissRequest = { hideSheetThen(GroupsPageAction.DismissBottomSheet) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val longPressedGroup = state.longPressedGroup
                val isOwner = longPressedGroup?.isGroupOwner(state.currentUid) != false
                Column(
                    modifier = Modifier.padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    longPressedGroup?.let { group ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp)
                        ) {
                            InitialTile(name = group.name, color = groupAccentColor(group.name))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = group.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = group.progressLabel(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (isOwner) {
                        BottomSheetRowItem(
                            icon = Icons.Default.Edit,
                            text = stringResource(id = R.string.edit_group),
                            onClick = {
                                hideSheetThen(GroupsPageAction.ShowEditGroupDialog(group = longPressedGroup ?: UIGroup()))
                            },
                            contentDescription = stringResource(id = R.string.edit_group),
                        )
                    }
                    BottomSheetRowItem(
                        icon = Icons.Default.Share,
                        text = stringResource(id = R.string.share_group),
                        onClick = {
                            hideSheetThen(GroupsPageAction.ShowShareGroupDialog(group = longPressedGroup ?: UIGroup()))
                        },
                        contentDescription = stringResource(id = R.string.share_group),
                    )
                    if (longPressedGroup != null &&
                        longPressedGroup.isShared &&
                        longPressedGroup.isGroupOwner(state.currentUid)
                    ) {
                        BottomSheetRowItem(
                            icon = Icons.Default.Group,
                            text = stringResource(id = R.string.group_members),
                            onClick = {
                                hideSheetThen(GroupsPageAction.ShowGroupMembersDialog(group = longPressedGroup))
                            },
                            contentDescription = stringResource(id = R.string.group_members),
                        )
                    }
                    if (isOwner) {
                        BottomSheetRowItem(
                            icon = Icons.Default.Archive,
                            text = stringResource(id = R.string.archive_group),
                            onClick = {
                                hideSheetThen(GroupsPageAction.ArchiveGroup(group = longPressedGroup ?: UIGroup()))
                            },
                            contentDescription = stringResource(id = R.string.archive_group),
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                    BottomSheetRowItem(
                        icon = Icons.Default.Delete,
                        text = stringResource(id = R.string.delete_group),
                        onClick = {
                            hideSheetThen(GroupsPageAction.DeleteGroup(group = longPressedGroup ?: UIGroup()))
                        },
                        contentDescription = stringResource(id = R.string.delete_group),
                        isDestructive = true,
                    )
                }
            }
        }
    }

}

@Composable
private fun GroupsHeader(
    groups: List<UIGroup>,
    pendingInvitationsCount: Int,
    onInvitationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsLeft = groups.sumOf { group -> group.articles.count { !it.bought } }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.your_groups),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (groups.isNotEmpty()) {
                Text(
                    text =
                        if (itemsLeft > 0) pluralStringResource(id = R.plurals.items_left_to_buy, count = itemsLeft, itemsLeft)
                        else stringResource(id = R.string.nothing_left_to_buy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        RoundIconButton(
            icon = Icons.Default.Mail,
            contentDescription = stringResource(id = R.string.invitations),
            badge = pendingInvitationsCount.takeIf { it > 0 }?.toString(),
            onClick = onInvitationsClick
        )
        Spacer(modifier = Modifier.width(8.dp))
        RoundIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(id = R.string.settings),
            onClick = onSettingsClick
        )
    }
}

@Composable
private fun AssistantCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(Color(0xFF1E8A52), Color(0xFF0F8F8A))))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(id = R.string.ai_agent),
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.ai_agent_card_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            Text(
                text = stringResource(id = R.string.ai_agent_card_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color.White
        )
    }
}

@Composable
fun GroupItem(
    group: UIGroup,
    onLongClick: () -> Unit,
    onIconClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    members: List<GroupMember> = emptyList(),
    currentUid: String? = null,
    dragHandleModifier: Modifier = Modifier
) {
    val otherMembers = members.filter { it.uid != currentUid }
    val accent = groupAccentColor(group.name)
    val boughtCount = group.articles.count { it.bought }
    val allBought = group.articles.allBought()
    val chevronRotation = if (group.expanded) 180f else 0f

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
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
                            onLongClick = onLongClick,
                            onClick = onClick
                        )
                        .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp)
                ) {
                    Box {
                        InitialTile(name = group.name, color = accent)
                        if (allBought) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(18.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (group.isShared) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = stringResource(id = R.string.shared),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(start = 6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = group.progressLabel(),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                if (allBought) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (group.isShared && otherMembers.isNotEmpty()) {
                        GroupMembersAvatars(
                            members = members,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onIconClick,
                    enabled = group.articles.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription =
                            if (group.expanded) stringResource(id = R.string.collapse)
                            else stringResource(id = R.string.expand),
                        modifier = Modifier.rotate(chevronRotation)
                    )
                }
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = null,
                    modifier = dragHandleModifier
                        .padding(end = 12.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            if (group.articles.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { boughtCount / group.articles.size.toFloat() },
                    color = if (allBought) MaterialTheme.colorScheme.primary else accent,
                    trackColor = accent.copy(alpha = 0.14f),
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                        .height(6.dp)
                )
            }
            AnimatedVisibility(
                visible = group.expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 6.dp)
                ) {
                    group.articles.forEach { article ->
                        GroupPreviewRow(article = article)
                    }
                    if (group.articles.any { it.price != null }) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.total),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = group.articles.sumOf { it.price ?: 0.0 }.tryToInt().toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun GroupPreviewRow(
    article: UIArticle,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (article.bought) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (article.bought) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = article.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (article.bought) TextDecoration.LineThrough else null,
            color =
                if (article.bought) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        article.price?.let { price ->
            Text(
                text = price.tryToInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupMembersAvatars(
    members: List<GroupMember>,
    modifier: Modifier = Modifier,
    maxVisible: Int = 3
) {
    val shown = members.take(maxVisible)
    val overflow = members.size - shown.size
    Row(
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
        modifier = modifier
    ) {
        shown.forEach { member ->
            EmailAvatar(
                email = member.email,
                size = 24.dp,
                fontSize = 10.sp,
                modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        if (overflow > 0) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            ) {
                Text(
                    text = "+$overflow",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NoGroups(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.ShoppingBasket,
        title = stringResource(id = R.string.no_groups_title),
        message = stringResource(id = R.string.no_groups_message),
        modifier = modifier
    )
}

@Composable
private fun UIGroup.progressLabel(): String = when {
    articles.isEmpty() -> stringResource(id = R.string.empty_group)
    articles.allBought() -> stringResource(id = R.string.all_bought)
    else -> stringResource(id = R.string.bought_progress, articles.count { it.bought }, articles.size)
}

private fun List<UIArticle>.allBought() = isNotEmpty() and all { it.bought }
