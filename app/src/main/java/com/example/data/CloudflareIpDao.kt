package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedIpDao {
    @Query("SELECT * FROM saved_ips ORDER BY latency ASC, downloadSpeed DESC")
    fun getAllSavedIps(): Flow<List<SavedIp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIp(savedIp: SavedIp)

    @Delete
    suspend fun deleteIp(savedIp: SavedIp)

    @Query("DELETE FROM saved_ips WHERE ipAddress = :ipAddress")
    suspend fun deleteByIp(ipAddress: String)

    @Query("DELETE FROM saved_ips")
    suspend fun clearAll()
}
