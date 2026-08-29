package com.example.articles.viewmodels

import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.text.input.TextFieldState
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.R
import com.example.articles.db.ArticleDatabase
import com.example.articles.models.ArticleUnit
import com.example.articles.models.UIArticle
import com.example.articles.models.UIGroup
import com.example.articles.models.UserPreferences
import com.example.articles.repositories.ArticleRepository
import com.example.articles.repositories.GroupRepository
import com.example.articles.repositories.InvitationRepository
import com.example.articles.utils.FileManager
import com.example.articles.fcm.SessionManager
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: ArticleDatabase,
    private val groupRepository: GroupRepository,
    private val articleRepository: ArticleRepository,
    private val invitationRepository: InvitationRepository,
    private val userPreferences: UserPreferences,
    private val triggerSync: TriggerSync,
    private val fileManager: FileManager,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private var state = MutableStateFlow(SettingsState())
    val uiState = state.asStateFlow()

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
            is SettingsAction.ImportGroups -> importGroups(action.uri)
            is SettingsAction.ClearImportResult -> clearImportResult()
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
        sessionManager.clearLocalDataAndSignOut()
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
                } else {
                    state.update { currentState ->
                        currentState.copy(
                            deleteAccountLoading = false,
                            errorMessage = task.exception?.message.toString()
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

    private fun exportGroups() = viewModelScope.launch(Dispatchers.IO) {
        val groups = groupRepository.getAllGroups().first()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val groupsArray = JSONArray()
        groups.forEach { group ->
            val articles = articleRepository.getArticlesByGroupId(group.id).first()
            val articlesArray = JSONArray()
            articles.forEach { article ->
                articlesArray.put(JSONObject().apply {
                    put("id", article.id.toString())
                    put("name", article.name)
                    article.quantity?.let { put("quantity", it) }
                    put("unit", context.getString(article.unit.abbreviation))
                    article.description?.takeIf { it.isNotBlank() }?.let { put("description", it) }
                    article.websiteLink?.takeIf { it.isNotBlank() }?.let { put("websiteLink", it) }
                    article.price?.let { put("price", it) }
                    put("bought", article.bought)
                    if (article.photoUri != null) put("photoFile", "photos/${article.id}.jpg")
                })
            }
            groupsArray.put(JSONObject().apply {
                put("id", group.id.toString())
                put("name", group.name)
                put("articles", articlesArray)
            })
        }
        val json = JSONObject().apply {
            put("exportDate", date)
            put("groups", groupsArray)
        }

        val zipFile = File(context.cacheDir, "articles_export_$date.zip")
        ZipOutputStream(zipFile.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()

            groups.forEach { group ->
                articleRepository.getArticlesByGroupId(group.id).first()
                    .filter { it.photoUri != null }
                    .forEach { article ->
                        runCatching {
                            val input = context.contentResolver
                                .openInputStream(Uri.parse(article.photoUri)) ?: return@runCatching
                            zip.putNextEntry(ZipEntry("photos/${article.id}.jpg"))
                            input.use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
            }
        }

        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", zipFile
        )
        state.update { it.copy(exportUri = uri) }
    }

    private fun importGroups(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        state.update { currentState ->
            currentState.copy(
                importLoading = true,
                importError = null,
                importSuccess = false
            )
        }

        val result = runCatching {
            var json: JSONObject? = null
            val photos = mutableMapOf<String, ByteArray>()

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        when {
                            entry.name == "data.json" ->
                                json = JSONObject(zip.readBytes().decodeToString())

                            entry.name.startsWith("photos/") ->
                                photos[entry.name.removePrefix("photos/")] = zip.readBytes()
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: error(context.getString(R.string.invalid_backup_file))

            val data = json ?: error(context.getString(R.string.invalid_backup_file))
            val groupsArray = data.getJSONArray("groups")
            for (i in 0 until groupsArray.length()) {
                val groupJson = groupsArray.getJSONObject(i)
                val groupId = UUID.fromString(groupJson.getString("id"))
                val groupAlreadyExistsLocally = groupRepository.getGroupByIdSuspend(groupId) != null

                val articlesArray = groupJson.getJSONArray("articles")
                val articlesToImport = (0 until articlesArray.length())
                    .map { articlesArray.getJSONObject(it) }
                    .filter { articleJson ->
                        val existing = articleRepository.getArticleByIdSuspend(
                            UUID.fromString(articleJson.getString("id"))
                        )
                        existing == null || existing.groupId == groupId
                    }

                if (!groupAlreadyExistsLocally && articlesToImport.isEmpty()) continue

                groupRepository.upsertGroup(
                    UIGroup(
                        id = groupId,
                        name = groupJson.getString("name")
                    )
                )

                articlesToImport.forEach { articleJson ->
                    val unitAbbreviation = articleJson.getString("unit")
                    val unit = ArticleUnit.entries.firstOrNull {
                        context.getString(it.abbreviation) == unitAbbreviation
                    } ?: ArticleUnit.default

                    val photoBytes = articleJson.optString("photoFile").takeIf { it.isNotBlank() }
                        ?.let { photos[it.removePrefix("photos/")] }
                    val photoUri = photoBytes?.let { bytes ->
                        val destUri = fileManager.photoUri()
                        context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                        destUri.toString()
                    }

                    articleRepository.upsertArticle(
                        UIArticle(
                            id = UUID.fromString(articleJson.getString("id")),
                            name = articleJson.getString("name"),
                            quantity = if (articleJson.has("quantity")) articleJson.getDouble("quantity") else null,
                            unit = unit,
                            description = articleJson.optString("description").takeIf { it.isNotBlank() },
                            websiteLink = articleJson.optString("websiteLink").takeIf { it.isNotBlank() },
                            price = if (articleJson.has("price")) articleJson.getDouble("price") else null,
                            bought = articleJson.optBoolean("bought", false),
                            photoUri = photoUri,
                            groupId = groupId
                        )
                    )
                }
            }
        }

        state.update {
            it.copy(
                importLoading = false,
                importSuccess = result.isSuccess,
                importError = result.exceptionOrNull()?.let { e -> e.message ?: e.toString() }
            )
        }
    }

    private fun clearImportResult() {
        state.update { it.copy(importSuccess = false, importError = null) }
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
            currentsState.copy(exportUri = null)
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
    val exportUri: Uri? = null,
    val deleteAccountLoading: Boolean = false,
    val signOutLoading: Boolean = false,
    val syncData: Boolean = true,
    val badgeValue: String? = null,
    val importLoading: Boolean = false,
    val importSuccess: Boolean = false,
    val importError: String? = null,
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
    data class ImportGroups(val uri: Uri) : SettingsAction
    data object ClearImportResult : SettingsAction
    data class UpdateSyncDataStatus(val syncData: Boolean) : SettingsAction
}
