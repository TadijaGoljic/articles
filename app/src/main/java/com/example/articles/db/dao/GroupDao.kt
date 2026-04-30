package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.articles.db.entities.GroupEntity
import com.example.articles.db.entities.GroupWithArticles
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GroupDao {

    @Insert suspend fun addNewGroup(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE idGroup = :idGroup")
    suspend fun delete(idGroup: UUID)

    @Transaction
    @Query("SELECT * FROM `groups`")
    fun getAllGroupsWithArticles(): Flow<List<GroupWithArticles>>

    @Transaction
    @Query("SELECT * FROM `groups` WHERE idGroup = :groupId")
    fun getGroupWithArticles(groupId: UUID): Flow<GroupWithArticles>

}