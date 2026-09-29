package com.example.articles.ui.composables.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.articles.R
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.ui.composables.utils.Toolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareGroupDialog(
    emailState: TextFieldState,
    isLoading: Boolean,
    errorMessage: String?,
    recentRecipients: List<String> = emptyList(),
    onRecentRecipientClick: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var isRecentsMenuExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            modifier = modifier
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Toolbar(title = stringResource(id = R.string.share_group))
                val query = emailState.text.toString()
                val filteredRecipients = remember(recentRecipients, query) {
                    if (query.isBlank()) recentRecipients
                    else recentRecipients.filter { it.contains(query, ignoreCase = true) }
                }
                ExposedDropdownMenuBox(
                    expanded = isRecentsMenuExpanded && filteredRecipients.isNotEmpty(),
                    onExpandedChange = { isRecentsMenuExpanded = it && filteredRecipients.isNotEmpty() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    OutlinedTextField(
                        state = emailState,
                        label = { Text(text = stringResource(id = R.string.enter_email_to_share)) },
                        supportingText = errorMessage?.let { { Text(text = it) } },
                        isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        trailingIcon = {
                            if (recentRecipients.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isRecentsMenuExpanded)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && filteredRecipients.isNotEmpty()) {
                                    isRecentsMenuExpanded = true
                                }
                            }
                    )
                    ExposedDropdownMenu(
                        expanded = isRecentsMenuExpanded && filteredRecipients.isNotEmpty(),
                        onDismissRequest = { isRecentsMenuExpanded = false }
                    ) {
                        filteredRecipients.forEach { email ->
                            DropdownMenuItem(
                                text = { Text(text = email) },
                                onClick = {
                                    onRecentRecipientClick(email)
                                    isRecentsMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(start = 24.dp, end = 8.dp)
                            .weight(1f)
                    ) {
                        Text(text = stringResource(id = R.string.cancel), fontSize = 14.sp)
                    }
                    ProgressButton(
                        onButtonClick = onConfirm,
                        isLoading = isLoading,
                        text = stringResource(id = R.string.send),
                        enabled = emailState.text.isNotEmpty() && !isLoading,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 24.dp)
                            .weight(1f)
                    )
                }
            }
        }
    }
}
