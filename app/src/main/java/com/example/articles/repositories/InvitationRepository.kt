package com.example.articles.repositories

import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.Invitation
import com.example.articles.models.InvitationStatus
import com.example.articles.models.UIGroup
import com.example.articles.utils.GROUPS_COLLECTION
import com.example.articles.utils.INVITATIONS_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            .collectionGroup("profile")
            .whereEqualTo("email", email)
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
            .mapNotNull { firestoreModelMapper.toInvitation(it) }
            .sortedByDescending { it.createdAt }
            .map { it.toEmail }
            .distinct()
            .take(limit)
    }

    suspend fun sendInvitation(
        fromUid: String,
        fromEmail: String,
        toEmail: String,
        toUid: String,
        group: UIGroup
    ) {
        val invitation = Invitation(
            invitationId = UUID.randomUUID().toString(),
            fromUid = fromUid,
            fromEmail = fromEmail,
            toEmail = toEmail,
            toUid = toUid,
            groupId = group.id.toString(),
            groupName = group.name,
            status = InvitationStatus.PENDING,
            createdAt = System.currentTimeMillis()
        )
        firestore.collection(INVITATIONS_COLLECTION)
            .document(invitation.invitationId)
            .set(firestoreModelMapper.toInvitationFirestoreMap(invitation))
            .await()
    }

    fun observePendingInvitations(uid: String): Flow<List<Invitation>> = callbackFlow {
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
    }

    suspend fun respondToInvitation(invitation: Invitation, accepted: Boolean) {
        if (accepted) {
            val sharedGroupId = sharedGroupRepository.createSharedGroup(
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

            val fromGroupRef = firestore.collection(USERS_COLLECTION)
                .document(invitation.fromUid)
                .collection(GROUPS_COLLECTION)
                .document(invitation.groupId)
            val fromGroupDoc = fromGroupRef.get().await()
            if (fromGroupDoc.exists()) {
                fromGroupRef.update(mapOf("isShared" to true, "sharedGroupId" to sharedGroupId)).await()
            } else {
                fromGroupRef.set(
                    mapOf(
                        "idGroup" to invitation.groupId,
                        "name" to invitation.groupName,
                        "isShared" to true,
                        "sharedGroupId" to sharedGroupId,
                        "updatedAt" to System.currentTimeMillis(),
                        "deleted" to false
                    ),
                    SetOptions.merge()
                ).await()
            }
        } else {
            firestore.collection(INVITATIONS_COLLECTION)
                .document(invitation.invitationId)
                .update("status", InvitationStatus.DECLINED.name)
                .await()
        }
    }

}
