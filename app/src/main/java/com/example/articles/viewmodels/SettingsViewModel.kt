package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.repositories.ArticleRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
) : ViewModel() {

    private var state = MutableStateFlow(SettingsState())
    val uiState = state

    fun signOut() = viewModelScope.launch {
        articleRepository.deleteAll()
        Firebase.auth.signOut()
        state.update { currentState ->
            currentState.copy(signOutOk = true)
        }
    }

    fun deleteAccount() = viewModelScope.launch {
        articleRepository.deleteAll()
        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                state.update { currentState ->
                    currentState.copy(deleteOk = true)
                }
            }
        }
    }

    fun showChangePasswordDialog() {
        state.update { currentState ->
            currentState.copy(showChangePasswordDialog = true)
        }
    }

    fun hideChangePasswordDialog() {
        state.update { currentState ->
            currentState.copy(
                showChangePasswordDialog = false,
                errorMessage = null
            )
        }
    }

    fun hideSignOutConfirmationDialog() {
        state.update { currentState ->
            currentState.copy(showSignOutConfirmationDialog = false)
        }
    }

    fun hideDeleteAccountConfirmationDialog() {
        state.update { currentState ->
            currentState.copy(showDeleteAccountConfirmationDialog = false)
        }
    }

    fun showSignOutConfirmationDialog() {
        state.update { currentState ->
            currentState.copy(showSignOutConfirmationDialog = true)
        }
    }

    fun showDeleteAccountConfirmationDialog() {
        state.update { currentState ->
            currentState.copy(showDeleteAccountConfirmationDialog = true)
        }
    }

    fun updateOldPassword(password: String) {
        state.update { currentState ->
            currentState.copy(oldPassword = password)
        }
    }

    fun updateNewPassword(password: String) {
        state.update { currentState ->
            currentState.copy(newPassword = password)
        }
    }

    fun changePassword() {
        if (state.value.newPassword.isEmpty() || state.value.oldPassword.isEmpty()) {
            state.update { currentState ->
                currentState.copy(stringResErrorMessage = R.string.fields_empty_error)
            }
            return
        }
        viewModelScope.launch {
            state.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    stringResErrorMessage = null,
                )
            }
            val credentials = EmailAuthProvider.getCredential(
                Firebase.auth.currentUser?.email.toString(),
                state.value.oldPassword
            )
            Firebase.auth.currentUser?.reauthenticate(credentials)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Firebase.auth.currentUser?.updatePassword(state.value.newPassword)
                            ?.addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) {
                                    state.update { currentState ->
                                        currentState.copy(isLoading = false)
                                    }
                                    hideChangePasswordDialog()
                                } else {
                                    state.update { currentState ->
                                        currentState.copy(
                                            isLoading = false,
                                            errorMessage = task.exception?.message.toString()
                                        )
                                    }
                                }
                            }
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
}

data class SettingsState(
    val signOutOk: Boolean = false,
    val deleteOk: Boolean = false,
    val showSignOutConfirmationDialog: Boolean = false,
    val showDeleteAccountConfirmationDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val isLoading: Boolean = false,
    @StringRes val stringResErrorMessage: Int? = null,
    val errorMessage: String? = null,
    val oldPassword: String = "",
    val newPassword: String = ""
)