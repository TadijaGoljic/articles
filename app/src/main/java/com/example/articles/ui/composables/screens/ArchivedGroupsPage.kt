package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.EmptyState
import com.example.articles.ui.composables.utils.InitialTile
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.composables.utils.groupAccentColor
import com.example.articles.utils.ScreenSize
import com.example.articles.utils.tryToInt
import com.example.articles.viewmodels.ArchivedGroupsAction
import com.example.articles.viewmodels.ArchivedGroupsViewModel

@Composable
fun ArchivedGroupsPage(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArchivedGroupsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val screenSize = ScreenSize.getScreenSize()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding)
        ) {
            Toolbar(
                title = stringResource(id = R.string.archived_groups),
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick
            )
            if (state.groups.isEmpty()) {
                NoArchivedGroups(modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                    items(state.groups) { group ->
                        ArchivedGroupItem(
                            group = group,
                            canUnarchive = group.isGroupOwner(state.currentUid),
                            onUnarchiveClick = {
                                viewModel.handleAction(ArchivedGroupsAction.UnarchiveGroup(group))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedGroupItem(
    group: UIGroup,
    canUnarchive: Boolean,
    onUnarchiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)
            ) {
                InitialTile(name = group.name, color = groupAccentColor(group.name))
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                    Text(
                        text =
                            if (group.articles.isEmpty()) stringResource(id = R.string.empty_group)
                            else stringResource(id = R.string.bought_progress, group.articles.count { it.bought }, group.articles.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canUnarchive) {
                    FilledTonalIconButton(onClick = onUnarchiveClick) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = stringResource(id = R.string.unarchive_group)
                        )
                    }
                }
            }
            if (group.articles.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 6.dp)
                ) {
                    group.articles.forEach { article ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = article.name,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (article.bought) TextDecoration.LineThrough else null,
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
                }
            }
        }
    }
}

@Composable
fun NoArchivedGroups(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.Inventory2,
        title = stringResource(id = R.string.no_archived_groups_title),
        message = stringResource(id = R.string.no_archived_groups_message),
        modifier = modifier
    )
}
