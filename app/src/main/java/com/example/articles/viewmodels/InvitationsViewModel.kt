package com.example.articles.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.ArticlesApplication
import com.example.articles.models.UIInvitation
import com.example.articles.repositories.InvitationRepository
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvitationsViewModel @Inject constructor(
    private val invitationRepository: InvitationRepository,
    private val triggerSync: TriggerSync
) : ViewModel() {

    private val state = MutableStateFlow(InvitationsState())
    val uiState = state.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            invitationRepository.observePendingInvitations(uid).collectLatest { invitations ->
                state.update { it.copy(invitation = invitations, isLoading = false) }
            }
        }
    }

    fun handleAction(action: InvitationsAction) {
        when (action) {
            is InvitationsAction.RespondToInvitation ->
                respondToInvitation(action.invitation, action.accepted)
        }
    }

    private fun respondToInvitation(invitation: UIInvitation, accepted: Boolean) {
        viewModelScope.launch {
            try {
                invitationRepository.respondToInvitation(invitation, accepted)
                if (accepted) triggerSync()
            } catch (e: Exception) {
                Log.e(ArticlesApplication.LOG_TAG, "respondToInvitation failed: ${e.message}", e)
            }
        }
    }

}

data class InvitationsState(
    val invitation: List<UIInvitation> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface InvitationsAction {
    data class RespondToInvitation(val invitation: UIInvitation, val accepted: Boolean) : InvitationsAction
}
