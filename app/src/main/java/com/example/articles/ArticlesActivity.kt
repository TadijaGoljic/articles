package com.example.articles

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.articles.models.UILocation
import com.example.articles.repositories.InvitationRepository
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.theme.ArticlesTheme
import com.example.articles.utils.PROFILES_COLLECTION
import com.example.articles.utils.normalizeEmail
import com.example.articles.utils.TriggerSync
import com.example.articles.fcm.isInvitationAlreadyNotified
import com.example.articles.fcm.markInvitationNotified
import com.example.articles.fcm.registerFcmToken
import com.example.articles.fcm.showInvitationNotification
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.contracts.contract

@AndroidEntryPoint
class ArticlesActivity : ComponentActivity() {

    @Inject lateinit var triggerSync: TriggerSync
    @Inject lateinit var invitationRepository: InvitationRepository

    private val newIntentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntentFlow.tryEmit(intent)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    companion object {
        private const val GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        writeUserProfile()
        Firebase.auth.currentUser?.uid?.let { uid -> registerFcmToken(this, uid) }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                triggerSync()
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(state = Lifecycle.State.STARTED) {
                val uid = Firebase.auth.currentUser?.uid ?: return@repeatOnLifecycle
                invitationRepository.observePendingInvitations(uid).collectLatest { invitations ->
                    invitations.forEach { invitation ->
                        if (!isInvitationAlreadyNotified(this@ArticlesActivity, invitation.invitationId)) {
                            showInvitationNotification(
                                context = this@ArticlesActivity,
                                invitationId = invitation.invitationId,
                                fromEmail = invitation.fromEmail,
                                groupName = invitation.groupName
                            )
                            markInvitationNotified(this@ArticlesActivity, invitation.invitationId)
                        }
                    }
                }
            }
        }
        setContent {
            ArticlesTheme {
                Application()
            }
        }
    }

    private fun writeUserProfile() {
        val user = Firebase.auth.currentUser ?: return
        Firebase.firestore
            .collection(PROFILES_COLLECTION)
            .document(user.uid)
            .set(
                mapOf(
                    "email" to user.email?.normalizeEmail(),
                    "uid" to user.uid
                ),
                SetOptions.merge()
            )
    }

    private fun openNavigation(location: UILocation) {
        val uri = buildString {
            append("${GOOGLE_MAPS_BASE_URL}/dir/?")
            append("api=1&")
            append("destination=${location.latitude},${location.longitude}&")
            append("travelmode=driving")
        }.toUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(GOOGLE_MAPS_PACKAGE_NAME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun openLink(url: String) =
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))

    private fun onSignOut() {
        finish()
        startActivity(Intent(this, AuthenticationActivity::class.java))
    }

    private fun onAccountDelete() = onSignOut()

    @Composable
    fun Application(
        modifier: Modifier = Modifier
    ) {
        ArticlesNavigationFlow(
            onSignOut = ::onSignOut,
            onAccountDelete = ::onAccountDelete,
            onOpenNavigation = ::openNavigation,
            onOpenLink = ::openLink,
            newIntentFlow = newIntentFlow,
            modifier = modifier
        )
    }

}