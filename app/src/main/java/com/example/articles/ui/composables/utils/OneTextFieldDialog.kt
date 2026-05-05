package com.example.articles.ui.composables.utils

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.articles.R

@Composable
fun OneTextFieldDialog(
    title: String,
    onDismissRequest: () -> Unit,
    textFieldState: TextFieldState,
    isError: Boolean,
    errorMessage: String,
    dismissButtonText: String,
    confirmButtonText: String,
    confirmButtonEnabled: Boolean,
    onConfirmButtonClick: () -> Unit,
    isConfirmButtonLoading: Boolean,
    modifier: Modifier = Modifier
) {

    val groupToCreateNameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        groupToCreateNameFocusRequester.requestFocus()
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
                Toolbar(
                    title = title
                )
                OutlinedTextField(
                    state = textFieldState,
                    label = { Text(text = stringResource(id = R.string.enter_group_name)) },
                    supportingText = if (isError) {
                        { Text(text = errorMessage) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .focusRequester(groupToCreateNameFocusRequester),
                    isError = isError
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Button(
                        onClick = onDismissRequest,
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
                            text = dismissButtonText,
                            fontSize = 14.sp
                        )
                    }
                    ProgressButton(
                        onButtonClick = onConfirmButtonClick,
                        isLoading = isConfirmButtonLoading,
                        text = confirmButtonText,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 16.dp)
                            .weight(weight = 4f),
                        enabled = confirmButtonEnabled
                    )
                }
            }
        }
    }

}