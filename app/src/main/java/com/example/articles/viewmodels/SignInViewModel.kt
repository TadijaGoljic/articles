package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    fun updateEmail(email: String) {
        state.update { currentState ->
            currentState.copy(
                email = email,
                errorMessage = null
            )
        }
    }

    fun updatePassword(password: String) {
        state.update { currentState ->
            currentState.copy(
                password = password,
                errorMessage = null
            )
        }
    }

    fun updateAuthenticationState(newState: AuthenticationState) {
        state.update { currentState ->
            currentState.copy(
                authenticationState = newState
            )
        }
    }

    fun hideAlert() {
        state.update { currentState ->
            currentState.copy(
                showAlert = false
            )
        }
    }

    private val auth = Firebase.auth
    var user by mutableStateOf(auth.currentUser)
        private set

    fun signIn() {
        state.update { currentState ->
            currentState.copy(
                isLoading = true
            )
        }
        auth.signInWithEmailAndPassword(state.value.email, state.value.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            ok = true
                        )
                    }
                } else {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorTitle = R.string.invalid_credentials,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

    fun signUp() {
        state.update { currentState ->
            currentState.copy(
                isLoading = true
            )
        }
        auth.createUserWithEmailAndPassword(state.value.email, state.value.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            ok = true
                        )
                    }
                } else {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorTitle = R.string.something_went_wrong,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

    fun forgotPassword() {
        state.update { currentState ->
            currentState.copy(
                isLoading = true
            )
        }
        auth.sendPasswordResetEmail(state.value.email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorTitle = R.string.we_sent_email,
                            showAlert = true
                        )
                    }
                } else {
                    state.update { currentState ->
                        currentState.copy(
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