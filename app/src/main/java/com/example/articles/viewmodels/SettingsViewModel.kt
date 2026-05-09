package com.example.articles.viewmodels

import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
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

    fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SignOut -> signOut()
            is SettingsAction.DeleteAccount -> deleteAccount()
            is SettingsAction.ShowChangePasswordDialog ->
                updateChangePasswordDialogVisibility(visible = true)
            is SettingsAction.HideChangePasswordDialog ->
                updateChangePasswordDialogVisibility(visible = false)
            is SettingsAction.ShowSignOutConfirmationDialog ->
                updateSignOutDialogVisibility(visible = true)
            is SettingsAction.HideSignOutConfirmationDialog ->
                updateSignOutDialogVisibility(visible = false)
            is SettingsAction.ShowDeleteAccountConfirmationDialog ->
                updateDeleteAccountDialogVisibility(visible = true)
            is SettingsAction.HideDeleteAccountConfirmationDialog ->
                updateDeleteAccountDialogVisibility(visible = false)
            is SettingsAction.ChangePassword -> changePassword()
        }
    }

    private fun signOut() = viewModelScope.launch {
        articleRepository.deleteAllArticles()
        Firebase.auth.signOut()
        state.update { currentState ->
            currentState.copy(signOutOk = true)
        }
    }

    private fun deleteAccount() = viewModelScope.launch {
        articleRepository.deleteAllArticles()
        Firebase
            .auth
            .currentUser
            ?.delete()
            ?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                state.update { it.copy(deleteOk = true) }
            }
        }
    }

    private fun updateChangePasswordDialogVisibility(visible: Boolean) {
        if (!visible) {
            state.value.oldPassword.edit { replace(0, length, "") }
            state.value.newPassword.edit { replace(0, length, "") }
        }
        state.update { currentState ->
            currentState.copy(
                showChangePasswordDialog = visible,
                errorMessage = null
            )
        }
    }

    private fun updateSignOutDialogVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(
                showSignOutConfirmationDialog = visible
            )
        }
    }

    private fun updateDeleteAccountDialogVisibility(visible: Boolean) {
        state.update { currentState ->
            currentState.copy(
                showDeleteAccountConfirmationDialog = visible
            )
        }
    }

    private fun changePassword() {
        if (state.value.newPassword.text.isEmpty() || state.value.oldPassword.text.isEmpty()) {
            state.update { it.copy(stringResErrorMessage = R.string.fields_empty_error) }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, stringResErrorMessage = null) }
            val credentials = EmailAuthProvider.getCredential(
                Firebase.auth.currentUser?.email.toString(),
                state.value.oldPassword.text.toString()
            )
            Firebase.auth.currentUser?.reauthenticate(credentials)
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Firebase.auth.currentUser?.updatePassword(state.value.newPassword.text.toString())
                            ?.addOnCompleteListener { task2 ->
                                if (task2.isSuccessful) {
                                    state.update { it.copy(isLoading = false) }
                                    handleAction(SettingsAction.HideChangePasswordDialog)
                                } else {
                                    state.update { it.copy(isLoading = false, errorMessage = task.exception?.message.toString()) }
                                }
                            }
                    } else {
                        state.update { it.copy(isLoading = false, errorMessage = task.exception?.message.toString()) }
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
    val oldPassword: TextFieldState = TextFieldState(""),
    val newPassword: TextFieldState = TextFieldState("")
)

sealed interface SettingsAction {
    data object SignOut : SettingsAction
    data object DeleteAccount : SettingsAction
    data object ShowChangePasswordDialog : SettingsAction
    data object HideChangePasswordDialog : SettingsAction
    data object ShowSignOutConfirmationDialog : SettingsAction
    data object HideSignOutConfirmationDialog : SettingsAction
    data object ShowDeleteAccountConfirmationDialog : SettingsAction
    data object HideDeleteAccountConfirmationDialog : SettingsAction
    data object ChangePassword : SettingsAction
}
