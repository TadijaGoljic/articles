package com.example.articles.ui.composables.screens

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.articles.ui.composables.utils.SettingsItemWithSwitch
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.ScreenSize
import com.example.articles.viewmodels.SettingsAction
import com.example.articles.viewmodels.SettingsState
import com.example.articles.viewmodels.SettingsViewModel
import com.example.articles.ui.composables.utils.SectionHeader
import com.example.articles.ui.composables.utils.EmailAvatar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    onBackClick: () -> Unit,
    onArchivedGroupsClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {

    val state by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val newPasswordFocusRequester = remember { FocusRequester() }
    val screenSize = ScreenSize.getScreenSize()
    val context = LocalContext.current
    var showImportConfirmationDialog by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { settingsViewModel.handleAction(SettingsAction.ImportGroups(it)) }
    }

    LaunchedEffect(state.exportUri) {
        state.exportUri?.let { uri ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
                Column {
                    Text(text = stringResource(id = R.string.confirm_delete_account))
                    OutlinedTextField(
                        state = state.deleteAccountPassword,
                        label = {
                            Text(text = stringResource(id = R.string.password))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        outputTransformation = OutputTransformation { replace(0, length, "•".repeat(length)) },
                        lineLimits = TextFieldLineLimits.SingleLine,
                        isError = state.errorMessage != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (state.showChangePasswordDialog) {
        Dialog(
            onDismissRequest = { settingsViewModel.handleAction(SettingsAction.HideChangePasswordDialog) },
        ) {
            Surface(
                modifier = Modifier.clip(shape = MaterialTheme.shapes.medium)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
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

    if (showImportConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showImportConfirmationDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirmationDialog = false
                        importLauncher.launch("application/zip")
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmationDialog = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.import_data))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_import))
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (state.importLoading) {
        Dialog(onDismissRequest = {}) {
            Surface(
                modifier = Modifier.clip(shape = MaterialTheme.shapes.medium)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }

    if (state.importSuccess || state.importError != null) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.handleAction(SettingsAction.ClearImportResult) },
            confirmButton = {
                TextButton(
                    onClick = { settingsViewModel.handleAction(SettingsAction.ClearImportResult) }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.import_data))
            },
            text = {
                Text(
                    text = if (state.importSuccess) {
                        stringResource(id = R.string.import_successful)
                    } else {
                        stringResource(id = R.string.import_failed, state.importError.orEmpty())
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                onImportGroups = { showImportConfirmationDialog = true },
                onArchivedGroupsClick = onArchivedGroupsClick,
                modifier = when (screenSize) {
                    ScreenSize.NORMAL -> Modifier
                    ScreenSize.LARGE -> Modifier.padding(horizontal = 40.dp)
                    ScreenSize.VERY_LARGE -> Modifier.padding(horizontal = 160.dp)
                },
                onSyncDataClick = { status ->
                    settingsViewModel.handleAction(SettingsAction.UpdateSyncDataStatus(status))
                },
                isSyncDataChecked = state.syncData,
                onConfirmAgentActionsClick = { status ->
                    settingsViewModel.handleAction(SettingsAction.UpdateConfirmAgentActionsStatus(status))
                },
                isConfirmAgentActionsChecked = state.confirmAgentActions
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
    onSyncDataClick: (Boolean) -> Unit,
    onExportGroups: () -> Unit,
    onImportGroups: () -> Unit,
    onArchivedGroupsClick: () -> Unit,
    isSyncDataChecked: Boolean,
    onConfirmAgentActionsClick: (Boolean) -> Unit,
    isConfirmAgentActionsChecked: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        if (userEmail.isNotEmpty()) {
            ProfileCard(email = userEmail)
            Spacer(modifier = Modifier.height(24.dp))
        }
        SectionHeader(text = stringResource(id = R.string.groups))
        SettingsItem(
            label = stringResource(id = R.string.archived_groups),
            icon = Icons.Default.Archive,
            onItemClick = onArchivedGroupsClick,
            position = Position.TOP,
        )
        SettingsItemWithSwitch(
            label = stringResource(id = R.string.confirm_ai_agent_actions),
            icon = Icons.Default.AutoAwesome,
            onItemClick = onConfirmAgentActionsClick,
            position = Position.BOTTOM,
            isChecked = isConfirmAgentActionsChecked,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(text = stringResource(id = R.string.data))
        SettingsItemWithSwitch(
            label = stringResource(id = R.string.sync_data),
            icon = Icons.Default.Sync,
            onItemClick = onSyncDataClick,
            position = Position.TOP,
            isChecked = isSyncDataChecked,
        )
        SettingsItem(
            label = stringResource(id = R.string.export_data),
            icon = Icons.Default.Upload,
            onItemClick = onExportGroups,
            position = Position.MIDDLE,
        )
        SettingsItem(
            label = stringResource(id = R.string.import_data),
            icon = Icons.Default.Download,
            onItemClick = onImportGroups,
            position = Position.BOTTOM,
        )
        Spacer(modifier = Modifier.height(24.dp))
        SectionHeader(text = stringResource(id = R.string.account))
        SettingsItem(
            label = stringResource(id = R.string.change_password),
            icon = Icons.Default.Lock,
            onItemClick = showChangePasswordDialog,
            position = Position.TOP,
        )
        SettingsItem(
            label = stringResource(id = R.string.sign_out),
            icon = Icons.AutoMirrored.Filled.Logout,
            onItemClick = showSignOutConfirmationDialog,
            position = Position.MIDDLE,
        )
        SettingsItem(
            label = stringResource(id = R.string.delete_account),
            icon = Icons.Default.DeleteForever,
            onItemClick = showDeleteAccountConfirmationDialog,
            position = Position.BOTTOM,
            isDestructive = true,
        )
    }
}

@Composable
private fun ProfileCard(
    email: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(20.dp)
    ) {
        EmailAvatar(email = email, size = 56.dp, fontSize = 24.sp)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = email.substringBefore("@"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
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
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
            )
        } else if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 14.sp,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Button(
                onClick = onDismissRequest,
                shape = MaterialTheme.shapes.medium,
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
                shape = MaterialTheme.shapes.medium
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