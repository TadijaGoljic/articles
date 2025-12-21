package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.viewmodels.SettingsState
import com.example.articles.viewmodels.SettingsViewModel

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onAccountDelete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {

    val state by settingsViewModel.uiState.collectAsState()
    val newPasswordFocusRequester = remember { FocusRequester() }

    LaunchedEffect(state.signOutOk) {
        if (state.signOutOk) onSignOut()
    }

    LaunchedEffect(state.deleteOk) {
        if (state.deleteOk) onAccountDelete()
    }

    if (state.showSignOutConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                settingsViewModel.hideSignOutConfirmationDialog()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.signOut()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.hideSignOutConfirmationDialog()
                    }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.sign_out))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_sign_out))
            }
        )
    }

    if (state.showDeleteAccountConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                settingsViewModel.hideDeleteAccountConfirmationDialog()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.deleteAccount()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.hideDeleteAccountConfirmationDialog()
                    }
                ) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.delete_account))
            },
            text = {
                Text(text = stringResource(id = R.string.confirm_delete_account))
            }
        )
    }

    if (state.showChangePasswordDialog) {
        Dialog(
            onDismissRequest = {
                settingsViewModel.hideChangePasswordDialog()
            },
        ) {
            Surface(
                modifier = Modifier.clip(shape = RoundedCornerShape(10.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    ChangePassword(
                        state = state,
                        onOldPasswordChange = settingsViewModel::updateOldPassword,
                        onNewPasswordChange = settingsViewModel::updateNewPassword,
                        onButtonClick = settingsViewModel::changePassword,
                        newPasswordFocusRequester = newPasswordFocusRequester,
                        onDismissRequest = settingsViewModel::hideChangePasswordDialog
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
            Text(
                text = stringResource(id = R.string.account),
                color = Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 2.dp)
            )
            SettingsItem(
                label = stringResource(id = R.string.sign_out),
                onItemClick = settingsViewModel::showSignOutConfirmationDialog,
                position = Position.TOP,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SettingsItem(
                label = stringResource(id = R.string.delete_account),
                onItemClick = settingsViewModel::showDeleteAccountConfirmationDialog,
                position = Position.MIDDLE,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            SettingsItem(
                label = stringResource(id = R.string.change_password),
                onItemClick = settingsViewModel::showChangePasswordDialog,
                position = Position.BOTTOM,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.notifications),
                color = Color.Gray,
                fontSize = 18.sp,
                modifier = Modifier
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 2.dp)
            )
            SettingsItem(
                label = stringResource(id = R.string.enable_notifications),
                onItemClick = {},
                position = Position.SINGLE,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

}

@Composable
fun SettingsItem(
    label: String,
    onItemClick: () -> Unit,
    position: Position,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(
                when (position) {
                    Position.SINGLE -> RoundedCornerShape(size = 10.dp)
                    Position.TOP -> RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                    Position.MIDDLE -> RoundedCornerShape(size = 0.dp)
                    Position.BOTTOM -> RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                }
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .clickable {
                onItemClick()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .height(height = 48.dp)
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        if (position != Position.SINGLE && position != Position.BOTTOM) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

enum class Position {
    TOP, MIDDLE, BOTTOM, SINGLE
}

@Composable
fun ChangePassword(
    state: SettingsState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
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
            value = state.oldPassword,
            onValueChange = onOldPasswordChange,
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
            keyboardActions = KeyboardActions(
                onNext = {
                    newPasswordFocusRequester.requestFocus()
                }
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            maxLines = 1,
            isError = state.errorMessage != null || state.stringResErrorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.newPassword,
            onValueChange = onNewPasswordChange,
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
            keyboardActions = KeyboardActions(
                onDone = {
                    onButtonClick()
                }
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            maxLines = 1,
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