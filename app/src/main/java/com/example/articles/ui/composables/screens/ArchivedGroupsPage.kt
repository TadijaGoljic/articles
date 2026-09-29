package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.UIGroup
import com.example.articles.ui.composables.utils.Toolbar
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
                NoArchivedGroups(
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
            } else {
                LazyColumn(
                    modifier = when (screenSize) {
                        ScreenSize.NORMAL -> Modifier
                        ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                        ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                    }
                ) {
                    items(state.groups) { group ->
                        ArchivedGroupItem(
                            group = group,
                            canUnarchive = group.isGroupOwner(state.currentUid),
                            onUnarchiveClick = {
                                viewModel.handleAction(ArchivedGroupsAction.UnarchiveGroup(group))
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${group.name} (${group.articles.size})",
                    fontSize = 20.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (canUnarchive) {
                    IconButton(onClick = onUnarchiveClick) {
                        Icon(
                            imageVector = Icons.Default.Unarchive,
                            contentDescription = stringResource(id = R.string.unarchive_group)
                        )
                    }
                }
            }
            if (group.articles.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.fillMaxWidth())
                Column {
                    group.articles.forEach { article ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .background(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = article.name,
                                modifier = Modifier.align(Alignment.CenterStart),
                            )
                            Text(
                                text = (article.price?.tryToInt() ?: "").toString(),
                                modifier = Modifier.align(Alignment.CenterEnd),
                            )
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
    Surface(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_archived_groups_title),
                color = Color.Gray,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(id = R.string.no_archived_groups_message),
                color = Color.Gray,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
