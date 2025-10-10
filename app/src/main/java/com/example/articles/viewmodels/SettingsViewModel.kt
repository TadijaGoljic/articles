package com.example.articles.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(

) : ViewModel() {

    private var state = MutableStateFlow(SettingsState())
    val uiState = state

    fun signOut() {
        Firebase.auth.signOut()
        state.update { currentState ->
            currentState.copy(
                ok = true
            )
        }
    }

}

data class SettingsState(
    val ok: Boolean = false,
)