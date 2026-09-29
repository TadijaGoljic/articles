package com.example.articles.repositories

import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIInvitation
import com.example.articles.models.InvitationStatus
import com.example.articles.models.UIGroup
import android.util.Log
import com.example.articles.ArticlesApplication
import com.example.articles.utils.INVITATIONS_COLLECTION
import com.example.articles.utils.PROFILES_COLLECTION
import com.example.articles.utils.normalizeEmail
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvitationRepository @Inject constructor(
    private val firestoreModelMapper: FirestoreModelMapper,
    private val sharedGroupRepository: SharedGroupRepository
) {

    private val firestore = Firebase.firestore

    suspend fun lookupUidByEmail(email: String): String? {
        val snapshot = firestore
            .collection(PROFILES_COLLECTION)
            .whereEqualTo("email", email.normalizeEmail())
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.getString("uid")
    }

    suspend fun getRecentShareRecipients(fromUid: String, limit: Int = 6): List<String> {
        val snapshot = firestore.collection(INVITATIONS_COLLECTION)
            .whereEqualTo("fromUid", fromUid)
            .get()
            .await()
        return snapshot.documents
            .asSequence()
            .mapNotNull { firestoreModelMapper.toInvitation(it) }
            .sortedByDescending { it.createdAt }
            .map { it.toEmail }
            .distinct()
            .take(limit)
            .toList()
    }

    suspend fun sendInvitation(
        fromUid: String,
        fromEmail: String,
        toEmail: String,
        toUid: String,
        group: UIGroup
    ) {
        val invitation = UIInvitation(
            invitationId = UUID.randomUUID().toString(),
            fromUid = fromUid,
            fromEmail = fromEmail,
            toEmail = toEmail,
            toUid = toUid,
            groupId = group.id.toString(),
            groupName = group.name,
            status = InvitationStatus.PENDING,
            createdAt = System.currentTimeMillis(),
            existingSharedGroupId = if (group.isShared) group.sharedGroupId else null
        )
        firestore.collection(INVITATIONS_COLLECTION)
            .document(invitation.invitationId)
            .set(firestoreModelMapper.toInvitationFirestoreMap(invitation))
            .await()
    }

    fun observePendingInvitations(uid: String): Flow<List<UIInvitation>> = callbackFlow {
        val registration = firestore.collection(INVITATIONS_COLLECTION)
            .whereEqualTo("toUid", uid)
            .whereEqualTo("status", InvitationStatus.PENDING.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val invitations = snapshot?.documents
                    ?.mapNotNull { firestoreModelMapper.toInvitation(it) }
                    ?: emptyList()
                trySend(invitations)
            }
        awaitClose { registration.remove() }
    }.catch { error ->
        Log.e(ArticlesApplication.LOG_TAG, "observePendingInvitations failed: ${error.message}", error)
        emit(emptyList())
    }

    suspend fun respondToInvitation(invitation: UIInvitation, accepted: Boolean) {
        if (accepted) {
            val sharedGroupId = invitation.existingSharedGroupId
                ?: sharedGroupRepository.createSharedGroup(
                    group = UIGroup(
                        id = UUID.fromString(invitation.groupId),
                        name = invitation.groupName
                    ),
                    fromUid = invitation.fromUid,
                    toUid = invitation.toUid
                )

            firestore.collection(INVITATIONS_COLLECTION)
                .document(invitation.invitationId)
                .update(mapOf(
                    "status" to InvitationStatus.ACCEPTED.name,
                    "sharedGroupId" to sharedGroupId
                ))
                .await()
        } else {
            firestore.collection(INVITATIONS_COLLECTION)
                .document(invitation.invitationId)
                .update("status", InvitationStatus.DECLINED.name)
                .await()
        }
    }

}
