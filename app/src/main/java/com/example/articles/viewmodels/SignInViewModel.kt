package com.example.articles.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
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
                            user = auth.currentUser
                        )
                    }
                } else {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = task.exception?.message.toString(),
                            showAlert = true
                        )
                    }
                }
            }
    }

    fun signUp() {
        auth.createUserWithEmailAndPassword(state.value.email, state.value.password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user = auth.currentUser
                } else {
                    state.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            errorMessage = task.exception?.message.toString()
                        )
                    }
                }
            }
    }

}

data class SignInState(
    val email: String = "tadijagoljic@gmail.com",
    val password: String = "hello2",
    val authenticationState: AuthenticationState = AuthenticationState.SIGN_IN,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val showAlert: Boolean = false
)

enum class AuthenticationState {
    SIGN_IN, SIGN_UP
}