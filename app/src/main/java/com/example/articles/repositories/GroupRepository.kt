package com.example.articles.repositories

import com.example.articles.db.dao.GroupDao
import com.example.articles.db.entities.GroupEntity
import com.example.articles.models.mappers.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.models.mappers.toGroupEntity
import com.example.articles.models.mappers.toUIGroup
import com.example.articles.utils.GROUPS_COLLECTION
import com.example.articles.utils.USERS_COLLECTION
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
    private val firestoreModelMapper: FirestoreModelMapper
) {

    private val firestore = Firebase.firestore

    private fun groupsCollection(uid: String) =
        firestore
            .collection(USERS_COLLECTION)
            .document(uid)
            .collection(GROUPS_COLLECTION)

    suspend fun upsertGroup(group: UIGroup) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val groupEntity = group.toGroupEntity()
        groupDao.upsertGroup(groupEntity)
        groupsCollection(uid)
            .document(group.id.toString())
            .set(firestoreModelMapper.toGroupsFirestoreMap(groupEntity))
            .await()
    }

    suspend fun deleteGroupById(groupId: UUID) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        groupDao.deleteGroupById(groupId)
        groupsCollection(uid)
            .document(groupId.toString())
            .delete()
            .await()
    }

    fun getAllGroupsNoMapping() = groupDao.getAllGroups()

    fun getAllGroups() = groupDao.getAllGroups().map { list ->
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

}
