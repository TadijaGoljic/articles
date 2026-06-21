package com.example.articles.repositories

import com.example.articles.db.dao.GroupDao
import com.example.articles.models.FirestoreModelMapper
import com.example.articles.models.UIGroup
import com.example.articles.models.toGroupEntity
import com.example.articles.models.toUIGroup
import com.example.articles.utils.GROUPS_COLLECTION
import com.google.firebase.Firebase
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

    suspend fun upsertGroup(group: UIGroup) {
        val groupEntity = group.toGroupEntity()
        groupDao.upsertGroup(groupEntity)
        firestore.collection(GROUPS_COLLECTION)
            .document(group.id.toString())
            .update(firestoreModelMapper.toGroupsFirestoreMap(groupEntity))
            .await()
    }

    suspend fun upsertAllGroups(groups: List<UIGroup>) = groups.forEach { group ->
        upsertGroup(group)
    }

    suspend fun deleteGroupById(groupId: UUID) {
        groupDao.deleteGroupById(groupId)
        firestore.collection(GROUPS_COLLECTION)
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

}
