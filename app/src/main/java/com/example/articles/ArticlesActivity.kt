package com.example.articles

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.articles.models.Invitation
import com.example.articles.models.Location
import com.example.articles.repositories.InvitationRepository
import com.example.articles.ui.composables.ArticlesNavigationFlow
import com.example.articles.ui.composables.INVITATIONS_DEEP_LINK
import com.example.articles.ui.theme.ArticlesTheme
import com.example.articles.utils.TriggerSync
import com.example.articles.utils.registerFcmToken
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        ActivityResultContracts.RequestPermission()
    ) {}

    companion object {
        private const val GOOGLE_MAPS_PACKAGE_NAME = "com.google.android.apps.maps"
        private const val GOOGLE_MAPS_BASE_URL = "https://www.google.com/maps"
        private const val NOTIFIED_INVITATIONS_PREFS = "notified_invitations"
        private const val NOTIFIED_INVITATIONS_KEY = "ids"
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
                        if (!isAlreadyNotified(invitation.invitationId)) {
                            showInvitationNotification(invitation)
                            markNotified(invitation.invitationId)
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

    private fun isAlreadyNotified(invitationId: String): Boolean {
        val prefs = getSharedPreferences(NOTIFIED_INVITATIONS_PREFS, MODE_PRIVATE)
        return prefs.getStringSet(NOTIFIED_INVITATIONS_KEY, emptySet())?.contains(invitationId) == true
    }

    private fun markNotified(invitationId: String) {
        val prefs = getSharedPreferences(NOTIFIED_INVITATIONS_PREFS, MODE_PRIVATE)
        val existing = prefs.getStringSet(NOTIFIED_INVITATIONS_KEY, emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.add(invitationId)
        prefs.edit { putStringSet(NOTIFIED_INVITATIONS_KEY, existing) }
    }

    private fun showInvitationNotification(invitation: Invitation) {
        val contentText = getString(
            R.string.new_invitation_notification,
            invitation.fromEmail,
            invitation.groupName
        )
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            INVITATIONS_DEEP_LINK.toUri(),
            this,
            ArticlesActivity::class.java
        )
        val pendingIntent = TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(this, ArticlesApplication.INVITATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(getString(R.string.invitations))
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        getSystemService<NotificationManager>()
            ?.notify(invitation.invitationId.hashCode(), notification)
    }

    private fun writeUserProfile() {
        val user = Firebase.auth.currentUser ?: return
        Firebase.firestore
            .collection("users").document(user.uid)
            .collection("profile").document("info")
            .set(mapOf("email" to user.email, "uid" to user.uid), SetOptions.merge())
    }

    private fun openNavigation(location: Location) {
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