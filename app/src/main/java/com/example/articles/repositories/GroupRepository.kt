package com.example.articles.repositories

import com.example.articles.db.dao.GroupDao
import com.example.articles.models.UIGroup
import com.example.articles.models.toGroupEntity
import com.example.articles.models.toUIGroup
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
) {

    suspend fun upsertGroup(group: UIGroup) =
        groupDao.upsertGroup(group.toGroupEntity())

    suspend fun deleteGroupById(groupId: UUID) =
        groupDao.deleteGroupById(groupId)

    fun getAllGroups() = groupDao.getAllGroups().map { list ->
        list.map { group -> group.toUIGroup() }
    }

    fun getGroupById(groupId: UUID) = groupDao.getGroupById(groupId)
        .mapNotNull { group -> group?.toUIGroup() }

}
