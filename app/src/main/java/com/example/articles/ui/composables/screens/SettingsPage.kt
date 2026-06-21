package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.content.Intent
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.articles.R
import com.example.articles.ui.composables.utils.Position
import com.example.articles.ui.composables.utils.SettingsItem
import com.example.articles.ui.composables.utils.SettingsItemWithBadge
import com.example.articles.ui.composables.utils.SettingsItemWithSwitch
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.ui.composables.utils.UserInfoItem
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.SettingsAction
import com.example.articles.viewmodels.SettingsState
import com.example.articles.viewmodels.SettingsViewModel

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    onBackClick: () -> Unit,
    onInvitationsClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {

    val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val newPasswordFocusRequester = remember { FocusRequester() }
    val screenSize = ScreenSize.getScreenSize()
    val context = LocalContext.current

    LaunchedEffect(state.exportContent) {
        state.exportContent?.let { content ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.export_data)))
            settingsViewModel.handleAction(SettingsAction.ClearExport)
        }
    }

    LaunchedEffect(state.signOutOk) {
        if (state.signOutOk) onSignOut()
    }

    LaunchedEffect(state.deleteOk) {
        if (state.deleteOk) onAccountDelete()
    }

    if (state.showSignOutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.handleAction(SettingsAction.HideSignOutConfirmationDialog) },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.handleAction(SettingsAction.SignOut) }
                ) {
                    if (state.signOutLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settingsViewModel.handleAction(SettingsAction.HideSignOutConfirmationDialog) }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.sign_out))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_sign_out))
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (state.showDeleteAccountConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.handleAction(SettingsAction.HideDeleteAccountConfirmationDialog) },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.handleAction(SettingsAction.DeleteAccount) }
                ) {
                    if (state.deleteAccountLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(text = stringResource(id = R.string.ok))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { settingsViewModel.handleAction(SettingsAction.HideDeleteAccountConfirmationDialog) }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.delete_account))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_delete_account))
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (state.showChangePasswordDialog) {
        Dialog(
            onDismissRequest = { settingsViewModel.handleAction(SettingsAction.HideChangePasswordDialog) },
        ) {
            Surface(
                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ChangePassword(
                        state = state,
                        onButtonClick = { settingsViewModel.handleAction(SettingsAction.ChangePassword) },
                        newPasswordFocusRequester = newPasswordFocusRequester,
                        onDismissRequest = { settingsViewModel.handleAction(SettingsAction.HideChangePasswordDialog) }
                    )
                }
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Toolbar(
                title = stringResource(id = R.string.settings),
                leadingIcon = Icons.Default.ChevronLeft,
                onLeadingIconClick = onBackClick,
                color = MaterialTheme.colorScheme.onBackground
            )
            SettingsItems(
                userEmail = state.userEmail,
                showSignOutConfirmationDialog = {
                    settingsViewModel.handleAction(SettingsAction.ShowSignOutConfirmationDialog)
                },
                showDeleteAccountConfirmationDialog = {
                    settingsViewModel.handleAction(SettingsAction.ShowDeleteAccountConfirmationDialog)
                },
                showChangePasswordDialog = {
                    settingsViewModel.handleAction(SettingsAction.ShowChangePasswordDialog)
                },
                onExportGroups = {
                    settingsViewModel.handleAction(SettingsAction.ExportGroups)
                },
                modifier = when (screenSize) {
                    ScreenSize.NORMAL -> Modifier
                    ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                    ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                },
                onSyncDataClick = { status ->
                    settingsViewModel.handleAction(SettingsAction.UpdateSyncDataStatus(status))
                },
                onInvitationsClick = onInvitationsClick,
                badgeValue = state.badgeValue,
                isSyncDataChecked = state.syncData
            )
        }
    }
}

@Composable
fun SettingsItems(
    userEmail: String,
    showSignOutConfirmationDialog: () -> Unit,
    showDeleteAccountConfirmationDialog: () -> Unit,
    showChangePasswordDialog: () -> Unit,
    onInvitationsClick: () -> Unit,
    onSyncDataClick: (Boolean) -> Unit,
    onExportGroups: () -> Unit,
    badgeValue: String?,
    isSyncDataChecked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        if (userEmail.isNotEmpty()) {
            UserInfoItem(
                email = userEmail,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = stringResource(id = R.string.account),
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(bottom = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        SettingsItem(
            label = stringResource(id = R.string.sign_out),
            onItemClick = showSignOutConfirmationDialog,
            position = Position.TOP,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        SettingsItem(
            label = stringResource(id = R.string.delete_account),
            onItemClick = showDeleteAccountConfirmationDialog,
            position = Position.MIDDLE,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        SettingsItem(
            label = stringResource(id = R.string.change_password),
            onItemClick = showChangePasswordDialog,
            position = Position.BOTTOM,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.data),
            color = Color.Gray,
            fontSize = 18.sp,
            modifier = Modifier
                .padding(horizontal = 14.dp)
                .padding(bottom = 2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        SettingsItemWithBadge(
            label = stringResource(id = R.string.invitations),
            onItemClick = onInvitationsClick,
            badgeValue = badgeValue,
            position = Position.TOP,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        SettingsItem(
            label = stringResource(id = R.string.export_data),
            onItemClick = onExportGroups,
            position = Position.MIDDLE,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        SettingsItemWithSwitch(
            label = stringResource(id = R.string.sync_data),
            onItemClick = onSyncDataClick,
            position = Position.BOTTOM,
            isChecked = isSyncDataChecked,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ChangePassword(
    state: SettingsState,
    onButtonClick: () -> Unit,
    onDismissRequest: () -> Unit,
    newPasswordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.change_password),
            fontSize = 30.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            state = state.oldPassword,
            placeholder = {
                Text(text = stringResource(id = R.string.old_password))
            },
            label = {
                Text(text = stringResource(id = R.string.old_password))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            onKeyboardAction = KeyboardActionHandler {
                newPasswordFocusRequester.requestFocus()
            },
            outputTransformation = OutputTransformation { replace(0, length, "•".repeat(length)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = state.errorMessage != null || state.stringResErrorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            state = state.newPassword,
            placeholder = {
                Text(text = stringResource(id = R.string.new_password))
            },
            label = {
                Text(text = stringResource(id = R.string.new_password))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            onKeyboardAction = KeyboardActionHandler {
                onButtonClick()
            },
            outputTransformation = OutputTransformation { replace(0, length, "•".repeat(length)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            isError = state.errorMessage != null || state.stringResErrorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(newPasswordFocusRequester)
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.stringResErrorMessage != null) {
            Text(
                text = stringResource(id = state.stringResErrorMessage),
                color = Color.Red,
                fontSize = 14.sp,
            )
        } else if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = Color.Red,
                fontSize = 14.sp,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(
                onClick = onDismissRequest,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    fontSize = 16.sp
                )
            }
            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(10.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(
                        text = stringResource(id = R.string.change_password),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}