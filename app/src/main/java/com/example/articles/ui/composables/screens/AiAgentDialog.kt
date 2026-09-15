package com.example.articles.ui.composables.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.articles.R
import com.example.articles.ai.GroupAgentCommand
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.ui.composables.utils.Toolbar
import com.example.articles.utils.rememberSpeechRecognizerState

@Composable
fun AiAgentDialog(
    promptState: TextFieldState,
    isProcessing: Boolean,
    errorMessage: String?,
    pendingCommand: GroupAgentCommand?,
    onDismissRequest: () -> Unit,
    onSubmit: (String) -> Unit,
    onConfirmPending: () -> Unit,
    onCancelPending: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val promptFocusRequester = remember { FocusRequester() }

    val speechRecognizerState = rememberSpeechRecognizerState(
        onResult = { text -> promptState.edit { replace(0, length, text) } },
        onError = {
            Toast.makeText(context, context.getString(R.string.something_went_wrong), Toast.LENGTH_SHORT).show()
        }
    )

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechRecognizerState.startListening()
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.ai_agent_mic_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(pendingCommand) {
        if (pendingCommand == null) promptFocusRequester.requestFocus()
    }

    Dialog(
        onDismissRequest = onDismissRequest
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            modifier = modifier
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Toolbar(title = stringResource(id = R.string.ai_agent))

                if (pendingCommand != null) {
                    Text(
                        text = pendingCommand.confirmationMessage(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                } else {
                    OutlinedTextField(
                        state = promptState,
                        placeholder = {
                            Text(text = stringResource(id = R.string.ai_agent_prompt_hint))
                        },
                        supportingText = {
                            if (errorMessage != null) Text(text = errorMessage)
                            else if (speechRecognizerState.listening) Text(text = stringResource(id = R.string.ai_agent_listening))
                        },
                        isError = errorMessage != null,
                        trailingIcon = {
                            IconButton(
                                onClick = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = stringResource(id = R.string.ai_agent)
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .focusRequester(promptFocusRequester)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Button(
                        onClick = if (pendingCommand != null) onCancelPending else onDismissRequest,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .padding(start = 16.dp, end = 8.dp)
                            .weight(weight = 3f)
                    ) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            fontSize = 14.sp
                        )
                    }
                    ProgressButton(
                        onButtonClick = {
                            if (pendingCommand != null) onConfirmPending()
                            else onSubmit(promptState.text.toString())
                        },
                        isLoading = isProcessing,
                        text =
                            if (pendingCommand != null) stringResource(id = R.string.confirm)
                            else stringResource(id = R.string.send),
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .weight(weight = 4f),
                        enabled = pendingCommand != null || promptState.text.isNotEmpty()
                    )
                }
            }
        }
    }

}

@Composable
private fun GroupAgentCommand.confirmationMessage(): String = when (this) {
    is GroupAgentCommand.CreateGroup -> stringResource(id = R.string.agent_confirm_create, group.name)
    is GroupAgentCommand.RenameGroup -> stringResource(id = R.string.agent_confirm_rename, group.name, newName)
    is GroupAgentCommand.DeleteGroup -> stringResource(id = R.string.agent_confirm_delete, group.name)
    is GroupAgentCommand.AddArticle -> stringResource(id = R.string.agent_confirm_add_article, article.name, group.name)
    is GroupAgentCommand.RenameArticle -> stringResource(id = R.string.agent_confirm_rename_article, article.name, newName)
    is GroupAgentCommand.RemoveArticle -> stringResource(id = R.string.agent_confirm_remove_article, article.name, groupName)
    is GroupAgentCommand.Unresolved -> message
}
