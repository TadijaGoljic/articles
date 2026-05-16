package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.articles.db.entities.GroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GroupDao {

    @Upsert suspend fun upsertGroup(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE idGroup = :idGroup")
    suspend fun deleteGroupById(idGroup: UUID)

    @Query("DELETE FROM `groups`")
    suspend fun deleteAllGroups()

    @Query("SELECT * FROM `groups`")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE idGroup = :idGroup")
    fun getGroupById(idGroup: UUID): Flow<GroupEntity?>

}