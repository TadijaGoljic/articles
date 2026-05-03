package com.example.articles.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.articles.db.entities.GroupEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface GroupDao {

    @Insert suspend fun addNewGroup(group: GroupEntity)

    @Query("DELETE FROM `groups` WHERE idGroup = :idGroup")
    suspend fun delete(idGroup: UUID)

    @Query("SELECT * FROM `groups`")
    fun getAllGroups(): Flow<List<GroupEntity>>

}