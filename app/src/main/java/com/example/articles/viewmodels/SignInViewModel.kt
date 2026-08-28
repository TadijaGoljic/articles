package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import com.example.articles.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(

) : ViewModel() {

    companion object {
        const val SIGN_UP_TAG = "SIGN_UP"
    }

    private var state = MutableStateFlow(SignInState())
    val uiState = state.asStateFlow()

    private val auth = Firebase.auth

    fun handleAction(action: SignInAction) {
        when (action) {
            is SignInAction.UpdateAuthenticationState -> updateAuthenticationState(action.authState)
            is SignInAction.HideAlert -> hideAlert()
            is SignInAction.SignIn -> signIn(action.email, action.password)
            is SignInAction.SignUp -> signUp(action.email, action.password)
            is SignInAction.ForgotPassword -> forgotPassword(action.email)
        }
    }

    private fun updateAuthenticationState(authState: AuthenticationState) {
        state.update { currentState ->
            currentState.copy(authenticationState = authState)
        }
    }

    private fun hideAlert() {
        state.update { currentState ->
            currentState.copy(showAlert = false)
        }
    }

    private fun signIn(email: String, password: String) {
        state.update { currentState -> currentState.copy(isLoading = true) }
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { it.copy(isLoading = false, ok = true) }
                } else {
                    state.update {
                        it.copy(
                            isLoading = false,
                            errorTitle = R.string.invalid_credentials,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

    private fun signUp(email: String, password: String) {
        state.update { it.copy(isLoading = true) }
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { it.copy(isLoading = false, ok = true) }
                } else {
                    state.update {
                        it.copy(
                            isLoading = false,
                            errorTitle = R.string.something_went_wrong,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

    private fun forgotPassword(email: String) {
        state.update { it.copy(isLoading = true) }
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { it.copy(isLoading = false, errorTitle = R.string.we_sent_email, showAlert = true) }
                } else {
                    state.update {
                        it.copy(
                            isLoading = false,
                            errorTitle = R.string.we_sent_email,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

}

data class SignInState(
    var email: TextFieldState = TextFieldState("tadijagoljic@gmail.com"),
    val password: TextFieldState = TextFieldState("oooooo"),
    val authenticationState: AuthenticationState = AuthenticationState.SIGN_IN,
    val errorMessage: String? = null,
    @StringRes val errorTitle: Int? = null,
    val isLoading: Boolean = false,
    val showAlert: Boolean = false,
    val ok: Boolean = false
)

enum class AuthenticationState {
    SIGN_IN, SIGN_UP, FORGOT_PASSWORD
}

sealed interface SignInAction {
    data class UpdateAuthenticationState(val authState: AuthenticationState) : SignInAction
    data object HideAlert : SignInAction
    data class SignIn(val email: String, val password: String) : SignInAction
    data class SignUp(val email: String, val password: String) : SignInAction
    data class ForgotPassword(val email: String) : SignInAction
}
