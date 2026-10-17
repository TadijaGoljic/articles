package com.example.articles.repositories

import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.models.mappers.toGroupEntity
import com.example.articles.models.mappers.toUIGroup
import com.example.articles.sync.RemoteContentWriter
import com.example.articles.utils.GROUPS_COLLECTION
import com.example.articles.utils.GROUP_ORDER_SUBCOLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper,
    private val remoteWriter: RemoteContentWriter
) {

    private val firestore = Firebase.firestore

    suspend fun upsertGroup(group: UIGroup) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val groupEntity = group.toGroupEntity()
        groupDao.upsertGroup(groupEntity)
        remoteWriter.set(
            firestore.collection(GROUPS_COLLECTION).document(group.id.toString()),
            firestoreModelMapper.toGroupsFirestoreMap(groupEntity.copy(ownerUid = uid))
        )
        syncGroupPosition(uid, group)
    }

    suspend fun upsertSharedGroupLocal(group: UIGroup) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        groupDao.upsertGroup(group.toGroupEntity())
        syncGroupPosition(uid, group)
    }

    private suspend fun syncGroupPosition(uid: String, group: UIGroup) {
        if (group.position != null) {
            pushGroupPosition(uid, group.id, group.position)
        } else if (group.isArchived) {
            deleteGroupPositionDoc(uid, group.id)
        }
    }

    suspend fun getNextGroupPosition(): Double =
        groupDao.getMaxActiveGroupPosition()?.plus(GAP) ?: GAP

    suspend fun getPositionForRestore(): Double =
        groupDao.getMinActiveGroupPosition()?.minus(GAP) ?: GAP

    suspend fun applyPulledGroupPosition(groupId: UUID, position: Double) {
        groupDao.applyServerPosition(groupId, position)
    }

    suspend fun updateGroupPosition(groupId: UUID, position: Double) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        groupDao.updatePosition(groupId, position, System.currentTimeMillis())
        pushGroupPosition(uid, groupId, position)
    }

    suspend fun renumberGroups(orderedIds: List<UUID>) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val updatedAt = System.currentTimeMillis()
        orderedIds.forEachIndexed { index, groupId ->
            val position = index * GAP
            groupDao.updatePosition(groupId, position, updatedAt)
            pushGroupPosition(uid, groupId, position)
        }
    }

    private suspend fun pushGroupPosition(uid: String, groupId: UUID, position: Double) {
        remoteWriter.set(groupPositionDoc(uid, groupId), firestoreModelMapper.toGroupOrderFirestoreMap(position))
    }

    private suspend fun deleteGroupPositionDoc(uid: String, groupId: UUID) {
        remoteWriter.delete(groupPositionDoc(uid, groupId))
    }

    private fun groupPositionDoc(uid: String, groupId: UUID) = firestore
        .collection(USERS_COLLECTION)
        .document(uid)
        .collection(GROUP_ORDER_SUBCOLLECTION)
        .document(groupId.toString())

    suspend fun deleteGroupById(groupId: UUID) {
        Firebase.auth.currentUser ?: return
        groupDao.deleteGroupById(groupId)
        remoteWriter.delete(firestore.collection(GROUPS_COLLECTION).document(groupId.toString()))
    }

    fun getAllGroupsNoMapping() = groupDao.getAllGroups()

    fun getAllGroups() = groupDao.getAllGroups().map { list ->
        list.map { group -> group.toUIGroup() }
    }

    fun getActiveGroups() = groupDao.getActiveGroups().map { list ->
        list.map { group -> group.toUIGroup() }
    }

    fun getArchivedGroups() = groupDao.getArchivedGroups().map { list ->
        list.map { group -> group.toUIGroup() }
    }

    fun getGroupById(groupId: UUID) = groupDao.getGroupById(groupId)
        .mapNotNull { group -> group?.toUIGroup() }

    suspend fun upsertGroupLocal(group: UIGroup) = groupDao.upsertGroup(group.toGroupEntity())

    suspend fun upsertAllGroupsLocal(groups: List<UIGroup>) = groups.forEach { upsertGroupLocal(it) }

    suspend fun deleteGroupLocalById(groupId: UUID) = groupDao.deleteGroupById(groupId)

    suspend fun hardDeleteLocalGroupsByIds(ids: List<UUID>) = groupDao.deleteGroupsByIds(ids)

    suspend fun getGroupBySharedGroupId(sharedGroupId: String): GroupEntity? =
        groupDao.getGroupBySharedGroupId(sharedGroupId)

    suspend fun getGroupByIdSuspend(groupId: UUID): GroupEntity? =
        groupDao.getGroupByIdSuspend(groupId)

    companion object {
        private const val GAP = 1000.0
    }

}
