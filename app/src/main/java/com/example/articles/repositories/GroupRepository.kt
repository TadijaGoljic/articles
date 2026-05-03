package com.example.articles.repositories

import com.example.articles.db.dao.GroupDao
import com.example.articles.models.UIGroup
import com.example.articles.models.toGroupEntity
import com.example.articles.models.toUIGroup
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupRepository @Inject constructor(
    private val groupDao: GroupDao,
) {

    suspend fun addNewGroup(group: UIGroup) =
        groupDao.addNewGroup(group.toGroupEntity())

    suspend fun delete(groupId: UUID) =
        groupDao.delete(groupId)

    fun getAllGroups() = groupDao.getAllGroups().map { list ->
        list.map { group -> group.toUIGroup() }
    }

}
