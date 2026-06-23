package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.BottomSheetRowItem
import com.example.articles.ui.composables.utils.OneTextFieldDialog
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.GroupsPageAction
import com.example.articles.viewmodels.GroupsPageViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsListPage(
    onGroupClick: (UIGroup) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    groupsPageViewModel: GroupsPageViewModel = hiltViewModel()
) {

    val state by groupsPageViewModel.uiState.collectAsStateWithLifecycle()
    val groupNames by remember {
        derivedStateOf {
            state.groups.map { group -> group.name.uppercase() }
        }
    }

    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val screenSize = ScreenSize.getScreenSize()

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
            confirmButtonEnabled = state.groupToCreateName.text.isNotEmpty(),
            onConfirmButtonClick = {
                groupsPageViewModel.handleAction(GroupsPageAction.SaveGroup(state.groupToCreateName.text.toString()))
            },
            isConfirmButtonLoading = state.isCreateGroupDialogLoading,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showShareGroupDialog) {
        OneTextFieldDialog(
            title = stringResource(id = R.string.share_group),
            onDismissRequest = {
                groupsPageViewModel.handleAction(GroupsPageAction.HideShareGroupDialog)
            },
            textFieldState = state.shareGroupEmail,
            isError = state.isShareGroupError,
            errorMessage =
                if (state.groupSharingErrorResource != null) stringResource(id = state.groupSharingErrorResource!!)
                else "",
            dismissButtonText = stringResource(id = R.string.cancel),
            confirmButtonText = stringResource(id = R.string.share_group),
            confirmButtonEnabled = state.shareGroupEmail.text.isNotEmpty(),
            onConfirmButtonClick = {
                groupsPageViewModel.handleAction(GroupsPageAction.ShareGroup(state.shareGroupEmail.text.toString()))
            },
            isConfirmButtonLoading = state.isCreateGroupDialogLoading,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    if (state.showEditGroupDialog) {
        val editNameUpper = state.groupToEditName.text.toString().uppercase()
        val originalNameUpper = state.longPressedGroup?.name?.uppercase()
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
            confirmButtonEnabled = state.groupToEditName.text.isNotEmpty(),
            onConfirmButtonClick = {
                groupsPageViewModel.handleAction(GroupsPageAction.EditGroup(state.longPressedGroup ?: UIGroup(), state.groupToEditName.text.toString()))
            },
            isConfirmButtonLoading = state.isEditDialogLoading,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    groupsPageViewModel.handleAction(GroupsPageAction.ShowAddNewGroupDialog)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.create_group)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Toolbar(
                title = stringResource(id = R.string.your_groups),
                leadingIcon = Icons.Default.Settings,
                onLeadingIconClick = onSettingsClick,
                leadingIconBadge = state.pendingInvitationsCount.toString(),
            )
            if (state.groups.isNotEmpty()) {
                LazyColumn(
                    modifier = when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                ) {
                    items(state.groups) { group ->
                        GroupItem(
                            group = group,
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
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else {
                NoGroups(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .then(
                            when (screenSize) {
                                ScreenSize.NORMAL -> Modifier
                                ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                                ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                            }
                        )
                        .height(78.dp)
                        .clip(shape = RoundedCornerShape(16.dp))
                )
            }
            if (state.showEditDeleteBottonSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        coroutineScope
                            .launch { sheetState.hide() }
                            .invokeOnCompletion {
                                groupsPageViewModel.handleAction(GroupsPageAction.DismissBottomSheet)
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
                            text = stringResource(id = R.string.edit_group),
                            onClick = {
                                coroutineScope
                                    .launch { sheetState.hide() }
                                    .invokeOnCompletion {
                                        groupsPageViewModel.handleAction(GroupsPageAction.ShowEditGroupDialog(group = state.longPressedGroup ?: UIGroup()))
                                    }
                            },
                            contentDescription = stringResource(id = R.string.edit_group),
                        )
                        BottomSheetRowItem(
                            icon = Icons.Default.Delete,
                            text = stringResource(id = R.string.delete_group),
                            onClick = {
                                coroutineScope
                                    .launch { sheetState.hide() }
                                    .invokeOnCompletion {
                                        groupsPageViewModel.handleAction(GroupsPageAction.DeleteGroup(group = state.longPressedGroup ?: UIGroup()))
                                    }
                            },
                            contentDescription = stringResource(id = R.string.delete_group),
                        )
                        BottomSheetRowItem(
                            icon = Icons.Default.Share,
                            text = stringResource(id = R.string.share_group),
                            onClick = {
                                coroutineScope
                                    .launch { sheetState.hide() }
                                    .invokeOnCompletion {
                                        groupsPageViewModel.handleAction(GroupsPageAction.ShowShareGroupDialog(group = state.longPressedGroup ?: UIGroup()))
                                    }
                            },
                            contentDescription = stringResource(id = R.string.share_group),
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun GroupItem(
    group: UIGroup,
    onLongClick: () -> Unit,
    onIconClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(10.dp))
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color =
                            if (group.articles.allBought()) Color.Green.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surface
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${group.name} (${group.articles.size})",
                    fontSize = 24.sp,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.CenterStart),
                    maxLines = 1
                )
                IconButton(
                    onClick = onIconClick,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterEnd),
                    enabled = group.articles.isNotEmpty()
                ) {
                    Icon(
                        imageVector =
                            if (group.expanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                        contentDescription =
                            if (group.expanded) stringResource(id = R.string.collapse)
                            else stringResource(id = R.string.expand),
                    )
                }
            }
            if (group.expanded) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    group.articles.forEach { article ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .then(
                                    if (article.bought) Modifier.background(color = Color.Green.copy(alpha = 0.2f))
                                    else Modifier
                                )
                        ) {
                            Text(
                                text = article.name,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun NoGroups(
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
                text = stringResource(id = R.string.no_groups_title),
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.no_groups_message),
                color = Color.Gray,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private fun List<UIArticle>.allBought() = isNotEmpty() and all { it.bought }