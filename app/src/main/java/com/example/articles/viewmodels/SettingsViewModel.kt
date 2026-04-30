package com.example.articles.viewmodels

import androidx.annotation.StringRes
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
            is SettingsAction.ShowChangePasswordDialog -> state.update { it.copy(showChangePasswordDialog = true) }
            is SettingsAction.HideChangePasswordDialog -> state.update { it.copy(showChangePasswordDialog = false, errorMessage = null) }
            is SettingsAction.ShowSignOutConfirmationDialog -> state.update { it.copy(showSignOutConfirmationDialog = true) }
            is SettingsAction.HideSignOutConfirmationDialog -> state.update { it.copy(showSignOutConfirmationDialog = false) }
            is SettingsAction.ShowDeleteAccountConfirmationDialog -> state.update { it.copy(showDeleteAccountConfirmationDialog = true) }
            is SettingsAction.HideDeleteAccountConfirmationDialog -> state.update { it.copy(showDeleteAccountConfirmationDialog = false) }
            is SettingsAction.UpdateOldPassword -> state.update { it.copy(oldPassword = action.password) }
            is SettingsAction.UpdateNewPassword -> state.update { it.copy(newPassword = action.password) }
            is SettingsAction.ChangePassword -> changePassword()
        }
    }

    private fun signOut() = viewModelScope.launch {
        articleRepository.deleteAll()
        Firebase.auth.signOut()
        state.update { it.copy(signOutOk = true) }
    }

    private fun deleteAccount() = viewModelScope.launch {
        articleRepository.deleteAll()
        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                state.update { it.copy(deleteOk = true) }
            }
        }
    }

    private fun changePassword() {
        if (state.value.newPassword.isEmpty() || state.value.oldPassword.isEmpty()) {
            state.update { it.copy(stringResErrorMessage = R.string.fields_empty_error) }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, stringResErrorMessage = null) }
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
    val oldPassword: String = "",
    val newPassword: String = ""
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
    data class UpdateOldPassword(val password: String) : SettingsAction
    data class UpdateNewPassword(val password: String) : SettingsAction
    data object ChangePassword : SettingsAction
}
