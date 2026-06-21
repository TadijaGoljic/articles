package com.example.articles.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.articles.models.Invitation
import com.example.articles.repositories.InvitationRepository
import com.example.articles.utils.TriggerSync
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val uiState = state

    init {
        viewModelScope.launch {
            val uid = Firebase.auth.currentUser?.uid ?: return@launch
            invitationRepository.observePendingInvitations(uid).collectLatest { invitations ->
                state.update { it.copy(invitations = invitations, isLoading = false) }
            }
        }
    }

    fun handleAction(action: InvitationsAction) {
        when (action) {
            is InvitationsAction.RespondToInvitation ->
                respondToInvitation(action.invitation, action.accepted)
        }
    }

    private fun respondToInvitation(invitation: Invitation, accepted: Boolean) {
        viewModelScope.launch {
            invitationRepository.respondToInvitation(invitation, accepted)
            if (accepted) triggerSync()
        }
    }

}

data class InvitationsState(
    val invitations: List<Invitation> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface InvitationsAction {
    data class RespondToInvitation(val invitation: Invitation, val accepted: Boolean) : InvitationsAction
}
