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

    @Query("DELETE FROM `groups` WHERE idGroup IN (:ids)")
    suspend fun deleteGroupsByIds(ids: List<UUID>)

    @Query("DELETE FROM `groups`")
    suspend fun deleteAllGroups()

    @Query("SELECT * FROM `groups` ORDER BY position IS NULL, position ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE isArchived = 0 ORDER BY position IS NULL, position ASC")
    fun getActiveGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM `groups` WHERE isArchived = 1 ORDER BY position IS NULL, position ASC")
    fun getArchivedGroups(): Flow<List<GroupEntity>>

    @Query("SELECT MAX(position) FROM `groups` WHERE isArchived = 0")
    suspend fun getMaxActiveGroupPosition(): Double?

    @Query("SELECT MIN(position) FROM `groups` WHERE isArchived = 0")
    suspend fun getMinActiveGroupPosition(): Double?

    @Query("UPDATE `groups` SET position = :position, updatedAt = :updatedAt WHERE idGroup = :idGroup")
    suspend fun updatePosition(idGroup: UUID, position: Double, updatedAt: Long)

    @Query("UPDATE `groups` SET position = :position, updatedAt = :updatedAt WHERE idGroup = :idGroup AND updatedAt < :updatedAt")
    suspend fun updatePositionIfNewer(idGroup: UUID, position: Double, updatedAt: Long)

    @Query("SELECT * FROM `groups` WHERE idGroup = :idGroup")
    fun getGroupById(idGroup: UUID): Flow<GroupEntity?>

    @Query("SELECT * FROM `groups` WHERE idGroup = :idGroup LIMIT 1")
    suspend fun getGroupByIdSuspend(idGroup: UUID): GroupEntity?

    @Query("SELECT * FROM `groups` WHERE sharedGroupId = :sharedGroupId")
    suspend fun getGroupBySharedGroupId(sharedGroupId: String): GroupEntity?

}
