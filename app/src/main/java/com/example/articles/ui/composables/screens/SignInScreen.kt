package com.example.articles.ui.composables.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.articles.R
import com.example.articles.ui.composables.utils.Logos
import com.example.articles.ui.composables.utils.ProgressButton
import com.example.articles.viewmodels.AuthenticationState
import com.example.articles.viewmodels.SignInState
import com.example.articles.viewmodels.SignInViewModel
import com.example.articles.viewmodels.SignInViewModel.Companion.SIGN_UP_TAG

@Composable
fun SignInScreen(
    onMoveToBackground: () -> Unit,
    onAuthenticationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    signInViewModel: SignInViewModel = hiltViewModel()
) {

    val state by signInViewModel.uiState.collectAsState()
    val passwordFocusRequester = remember { FocusRequester() }
    val configuration = LocalConfiguration.current

    BackHandler {
        if (state.authenticationState == AuthenticationState.SIGN_IN) onMoveToBackground()
        else signInViewModel.updateAuthenticationState(AuthenticationState.SIGN_IN)
    }

    LaunchedEffect(state.ok) {
        if (state.ok) onAuthenticationComplete()
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

    if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        SignInPortrait(
            state = state,
            onEmailChange = signInViewModel::updateEmail,
            onPasswordChange = signInViewModel::updatePassword,
            onSignInClick = signInViewModel::signIn,
            onSignUpClick = signInViewModel::signUp,
            onForgotPasswordClick = signInViewModel::forgotPassword,
            updateAuthenticationState = signInViewModel::updateAuthenticationState,
            passwordFocusRequester = passwordFocusRequester,
            modifier = modifier
        )
    } else {
        SignInLandscape(
            state = state,
            onEmailChange = signInViewModel::updateEmail,
            onPasswordChange = signInViewModel::updatePassword,
            onSignInClick = signInViewModel::signIn,
            onSignUpClick = signInViewModel::signUp,
            onForgotPasswordClick = signInViewModel::forgotPassword,
            updateAuthenticationState = signInViewModel::updateAuthenticationState,
            passwordFocusRequester = passwordFocusRequester,
            modifier = modifier
        )
    }

}

@Composable
fun SignInPortrait(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    updateAuthenticationState: (AuthenticationState) -> Unit,
    passwordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Logos(
            showBlue = true,
            showGreen = true
        )
        SignInScreenAction(
            state = state,
            onEmailChange = onEmailChange,
            onPasswordChange = onPasswordChange,
            onSignInClick = onSignInClick,
            onSignUpClick = onSignUpClick,
            onForgotPasswordClick = onForgotPasswordClick,
            updateAuthenticationState = updateAuthenticationState,
            passwordFocusRequester = passwordFocusRequester,
        )
    }
}

@Composable
fun SignInLandscape(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    updateAuthenticationState: (AuthenticationState) -> Unit,
    passwordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logos(
                showBlue = true,
                showGreen = true
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SignInScreenAction(
                state = state,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onSignInClick = onSignInClick,
                onSignUpClick = onSignUpClick,
                onForgotPasswordClick = onForgotPasswordClick,
                updateAuthenticationState = updateAuthenticationState,
                passwordFocusRequester = passwordFocusRequester,
            )
        }
    }
}

@Composable
fun SignInScreenAction(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    updateAuthenticationState: (AuthenticationState) -> Unit,
    passwordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        modifier = modifier,
        targetState = state.authenticationState,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            slideInHorizontally(
                animationSpec = tween(300)
            ) { fullWidth -> fullWidth * direction } togetherWith
                    slideOutHorizontally(
                        animationSpec = tween(300)
                    ) { fullWidth -> -fullWidth * direction }
        },
    ) { target ->
        when (target) {
            AuthenticationState.SIGN_IN -> SignIn(
                state = state,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onButtonClick = onSignInClick,
                passwordFocusRequester = passwordFocusRequester,
                updateAuthenticationState = updateAuthenticationState,
            )
            AuthenticationState.SIGN_UP -> SignUp(
                state = state,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onButtonClick = onSignUpClick,
                passwordFocusRequester = passwordFocusRequester,
            )
            AuthenticationState.FORGOT_PASSWORD -> ForgotPassword(
                state = state,
                onEmailChange = onEmailChange,
                onButtonClick = onForgotPasswordClick,
            )
        }
    }
}

@Composable
fun SignIn(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onButtonClick: () -> Unit,
    updateAuthenticationState: (AuthenticationState) -> Unit,
    passwordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {

    val annotatedString = buildAnnotatedString {
        withStyle(style = SpanStyle(color = Color.Gray, fontSize = 16.sp)) {
            append("${stringResource(id = R.string.not_a_member)} ")
        }
        pushStringAnnotation(tag = SIGN_UP_TAG, annotation = "sign_up")
        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)) {
            append(stringResource(id = R.string.sign_up))
        }
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.sign_in),
            fontSize = 50.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
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
            onValueChange = onPasswordChange,
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
                    onButtonClick()
                }
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            maxLines = 1,
            isError = state.errorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
        )
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .clickable(
                    role = Role.Button
                ) {
                    updateAuthenticationState(AuthenticationState.SIGN_UP)
                }
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(
            onClick = {
                updateAuthenticationState(AuthenticationState.FORGOT_PASSWORD)
            }
        ) {
            Text(
                text = stringResource(id = R.string.forgot_password),
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
        Spacer(modifier = Modifier.height(height = 60.dp))
        ProgressButton(
            onButtonClick = onButtonClick,
            isLoading = state.isLoading,
            text = stringResource(id = R.string.sign_in)
        )
    }

}

@Composable
fun SignUp(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onButtonClick: () -> Unit,
    passwordFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.sign_up),
            fontSize = 50.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
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
            onValueChange = onPasswordChange,
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
                    onButtonClick()
                }
            ),
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            maxLines = 1,
            isError = state.errorMessage != null,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
        )
        Spacer(modifier = Modifier.height(height = 179.dp))
        ProgressButton(
            onButtonClick = onButtonClick,
            isLoading = state.isLoading,
            text = stringResource(id = R.string.sign_up)
        )
    }
}

@Composable
fun ForgotPassword(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.forgot_password),
            fontSize = 46.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChange,
            placeholder = {
                Text(text = stringResource(id = R.string.email))
            },
            label = {
                Text(text = stringResource(id = R.string.email))
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    onButtonClick()
                }
            ),
            singleLine = true,
            maxLines = 1,
            isError = state.errorMessage != null,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(height = 247.dp))
        ProgressButton(
            onButtonClick = onButtonClick,
            isLoading = state.isLoading,
            text = stringResource(id = R.string.forgot_password)
        )
    }
}