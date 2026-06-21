package com.example.articles.viewmodels

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.db.ArticleDatabase
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: ArticleDatabase,
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val invitationRepository: InvitationRepository,
    private val userPreferences: UserPreferences,
    private val triggerSync: TriggerSync,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var state = MutableStateFlow(SettingsState())
    val uiState = state

    init {
        val currentUser = Firebase.auth.currentUser
        state.update { it.copy(userEmail = currentUser?.email ?: "") }
        viewModelScope.launch {
            val uid = currentUser?.uid ?: ""
            invitationRepository.observePendingInvitations(uid).collect { invitations ->
                state.update { currentState ->
                    currentState.copy(
                        badgeValue = invitations.size.toString(),
                        syncData = userPreferences.syncDataEnabled
                    )
                }
            }
        }
    }

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
            is SettingsAction.ExportGroups -> exportGroups()
            is SettingsAction.ClearExport -> cleanExport()
            is SettingsAction.UpdateSyncDataStatus ->
                updateSyncDataStatus(syncData = action.syncData)
        }
    }

    private fun signOut() = viewModelScope.launch {
        state.update { currentState ->
            currentState.copy(
                signOutLoading = true
            )
        }
        withContext(Dispatchers.IO) { database.clearAllTables() }
        Firebase.auth.signOut()
        state.update { currentState ->
            currentState.copy(
                signOutLoading = false,
                signOutOk = true
            )
        }
    }

    private fun deleteAccount() = viewModelScope.launch {
        state.update { currentState ->
            currentState.copy(
                deleteAccountLoading = true
            )
        }
        val uid = Firebase.auth.currentUser?.uid
        withContext(Dispatchers.IO) { database.clearAllTables() }
        if (uid != null) {
            val userRef = Firebase.firestore.collection("users").document(uid)
            userRef.delete().await()
        }
        Firebase.auth.currentUser
            ?.delete()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    state.update { currentState ->
                        currentState.copy(
                            deleteAccountLoading = false,
                            deleteOk = true
                        )
                    }
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

    private fun exportGroups() = viewModelScope.launch {
        val groups = groupRepository.getAllGroups().first().filter { !it.deleted }
        val sb = StringBuilder()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        sb.appendLine("Articles Export")
        sb.appendLine("Exported: $date")
        groups.forEach { group ->
            sb.appendLine()
            sb.appendLine("== ${group.name} ==")
            val articles = articleRepository.getArticlesByGroupId(group.id).first()
                .filter { !it.deleted }
            articles.forEach { article ->
                val quantity = article.quantity
                val unit = context.getString(article.unit.abbreviation)
                val quantityStr =
                    if (quantity != null) " (${formatQuantity(quantity)} $unit)" else ""
                sb.appendLine("• ${article.name}$quantityStr")
                if (!article.description.isNullOrBlank()) {
                    sb.appendLine("  ${article.description}")
                }
            }
        }
        state.update { it.copy(exportContent = sb.toString()) }
    }

    private fun formatQuantity(quantity: Double): String =
        if (quantity == quantity.toLong().toDouble()) quantity.toLong().toString()
        else quantity.toString()

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
                                    state.update {
                                        it.copy(
                                            isLoading = false,
                                            errorMessage = task.exception?.message.toString()
                                        )
                                    }
                                }
                            }
                    } else {
                        state.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = task.exception?.message.toString()
                            )
                        }
                    }
                }
        }
    }

    private fun cleanExport() {
        state.update { currentsState ->
            currentsState.copy(exportContent = null)
        }
    }

    private fun updateSyncDataStatus(syncData: Boolean) {
        state.update { currentState ->
            currentState.copy(
                syncData = syncData
            )
        }
        userPreferences.syncDataEnabled = syncData
        triggerSync()
    }

}

data class SettingsState(
    val userEmail: String = "",
    val signOutOk: Boolean = false,
    val deleteOk: Boolean = false,
    val showSignOutConfirmationDialog: Boolean = false,
    val showDeleteAccountConfirmationDialog: Boolean = false,
    val showChangePasswordDialog: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val oldPassword: TextFieldState = TextFieldState(""),
    val newPassword: TextFieldState = TextFieldState(""),
    val exportContent: String? = null,
    val deleteAccountLoading: Boolean = false,
    val signOutLoading: Boolean = false,
    val syncData: Boolean = true,
    val badgeValue: String? = null,
    @StringRes val stringResErrorMessage: Int? = null
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
    data object ExportGroups : SettingsAction
    data object ClearExport : SettingsAction
    data class UpdateSyncDataStatus(val syncData: Boolean) : SettingsAction
}
