package com.example.articles.ui.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.clearCompositionErrors
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.viewmodels.AuthenticationState
import com.example.articles.viewmodels.SignInViewModel
import com.example.articles.viewmodels.SignInViewModel.Companion.SIGN_UP_TAG
import com.google.firebase.auth.FirebaseUser

@Composable
fun SignIn(
    onAuthenticationComplete: (FirebaseUser) -> Unit,
    modifier: Modifier = Modifier,
    signInViewModel: SignInViewModel = hiltViewModel()
) {

    val state by signInViewModel.uiState.collectAsState()
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 16.sp)) {
            append("${stringResource(id = R.string.not_a_member)} ")
        }
        pushStringAnnotation(tag = SIGN_UP_TAG, annotation = "sign_up")
        withStyle(style = SpanStyle(color = Color.Blue, fontSize = 16.sp)) {
            append(stringResource(id = R.string.sign_up))
        }
    }

    val onButtonClick: () -> Unit = {
        if (state.authenticationState == AuthenticationState.SIGN_IN) signInViewModel.signIn()
        else signInViewModel.signUp()
    }

    LaunchedEffect(state.user) {
        state.user?.let { user -> onAuthenticationComplete(user) }
    }

    if (state.showAlert) {
        AlertDialog(
            onDismissRequest = {
                signInViewModel.hideAlert()
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        signInViewModel.hideAlert()
                    }
                ) {
                    Text(text = stringResource(id = R.string.ok))
                }
            },
            title = {
                Text(text = stringResource(id = R.string.invalid_credentials))
            },
            text = {
                Text(text = state.errorMessage.orEmpty())
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = state.email,
            onValueChange = signInViewModel::updateEmail,
            placeholder = {
                Text(text = stringResource(id = R.string.email))
            },
            label = {
                Text(text = stringResource(id = R.string.email))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = {
                    passwordFocusRequester.requestFocus()
                }
            ),
            singleLine = true,
            maxLines = 1,
            isError = state.errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.password,
            onValueChange = signInViewModel::updatePassword,
            placeholder = {
                Text(text = stringResource(id = R.string.password))
            },
            label = {
                Text(text = stringResource(id = R.string.password))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onButtonClick()
                }
            ),
            visualTransformation = PasswordVisualTransformation(),
            isError = state.errorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
        )
        if (state.authenticationState == AuthenticationState.SIGN_IN) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ClickableText(
                    text = annotatedString,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = SIGN_UP_TAG,
                            start = offset,
                            end = offset
                        ).firstOrNull()?.let { _ ->
                            signInViewModel.updateAuthenticationState(AuthenticationState.SIGN_UP)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(
                    onClick = {

                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.forgot_password),
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onButtonClick,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth()
                .size(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Text(
                    text =
                        if (state.authenticationState == AuthenticationState.SIGN_IN) stringResource(id = R.string.sign_in)
                        else stringResource(id = R.string.sign_up),
                    fontSize = 20.sp
                )
            }
        }
    }

}