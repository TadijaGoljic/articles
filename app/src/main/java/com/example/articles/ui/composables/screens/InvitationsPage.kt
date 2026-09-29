package com.example.articles.ui.composables.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.models.UIInvitation
import com.example.articles.ui.composables.utils.EmailAvatar
import com.example.articles.ui.composables.utils.EmptyState
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.InvitationsAction
import com.example.articles.viewmodels.InvitationsViewModel
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun InvitationsPage(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitationsViewModel = hiltViewModel()
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
                title = stringResource(id = R.string.invitations),
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick
            )
            if (state.invitation.isEmpty() && !state.isLoading) {
                NoInvitations(modifier = Modifier.fillMaxWidth())
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
                    items(state.invitation) { invitation ->
                        InvitationCard(
                            invitation = invitation,
                            onAccept = {
                                viewModel.handleAction(InvitationsAction.RespondToInvitation(invitation, true))
                            },
                            onDecline = {
                                viewModel.handleAction(InvitationsAction.RespondToInvitation(invitation, false))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: UIInvitation,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", LocalLocale.current.platformLocale)
    val formattedDate = dateFormat.format(Date(invitation.createdAt))

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                EmailAvatar(email = invitation.fromEmail, size = 40.dp, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            id = R.string.invitation_from,
                            invitation.fromEmail,
                            invitation.groupName
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Text(text = stringResource(id = R.string.decline))
                }
                Button(
                    onClick = onAccept,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                ) {
                    Text(text = stringResource(id = R.string.accept))
                }
            }
        }
    }
}

@Composable
fun NoInvitations(
    modifier: Modifier = Modifier
) {
    EmptyState(
        icon = Icons.Outlined.MarkEmailRead,
        title = stringResource(id = R.string.no_pending_invitations),
        modifier = modifier
    )
}
