package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.example.articles.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(

) : ViewModel() {

    companion object {
        const val SIGN_UP_TAG = "SIGN_UP"
    }

    private var state = MutableStateFlow(SignInState())
    val uiState = state

    private val auth = Firebase.auth

    fun handleAction(action: SignInAction) {
        when (action) {
            is SignInAction.UpdateEmail -> state.update { it.copy(email = action.email, errorMessage = null) }
            is SignInAction.UpdatePassword -> state.update { it.copy(password = action.password, errorMessage = null) }
            is SignInAction.UpdateAuthenticationState -> state.update { it.copy(authenticationState = action.authState) }
            is SignInAction.HideAlert -> state.update { it.copy(showAlert = false) }
            is SignInAction.SignIn -> signIn()
            is SignInAction.SignUp -> signUp()
            is SignInAction.ForgotPassword -> forgotPassword()
        }
    }

    private fun signIn() {
        state.update { it.copy(isLoading = true) }
        auth.signInWithEmailAndPassword(state.value.email, state.value.password)
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

    private fun signUp() {
        state.update { it.copy(isLoading = true) }
        auth.createUserWithEmailAndPassword(state.value.email, state.value.password)
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

    private fun forgotPassword() {
        state.update { it.copy(isLoading = true) }
        auth.sendPasswordResetEmail(state.value.email)
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
    val email: String = "tadijagoljic@gmail.com",
    val password: String = "oooooo",
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
    data class UpdateEmail(val email: String) : SignInAction
    data class UpdatePassword(val password: String) : SignInAction
    data class UpdateAuthenticationState(val authState: AuthenticationState) : SignInAction
    data object HideAlert : SignInAction
    data object SignIn : SignInAction
    data object SignUp : SignInAction
    data object ForgotPassword : SignInAction
}
