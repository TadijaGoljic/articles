package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.repositories.ArticleRepository
import com.google.firebase.Firebase
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
            currentState.copy(ok = true)
        }
    }

    fun deleteAccount() = viewModelScope.launch {
        articleRepository.deleteAll()
        Firebase.auth.currentUser?.delete()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                state.update { currentState ->
                    currentState.copy(ok = true)
                }
            }
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

}

data class SettingsState(
    val ok: Boolean = false,
    val showSignOutConfirmationDialog: Boolean = false,
    val showDeleteAccountConfirmationDialog: Boolean = false
)