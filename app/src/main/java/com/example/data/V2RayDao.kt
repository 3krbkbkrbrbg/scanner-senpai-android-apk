package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface V2RayDao {

    // Subscriptions
    @Query("SELECT * FROM subscriptions ORDER BY id DESC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: Subscription)

    @Delete
    suspend fun deleteSubscription(sub: Subscription)

    @Query("SELECT * FROM subscriptions")
    suspend fun getSubscriptionsList(): List<Subscription>

    @Query("UPDATE subscriptions SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateSubTimestamp(id: Long, timestamp: Long)

    // Configs
    @Query("SELECT * FROM v2ray_configs ORDER BY latency ASC, id DESC")
    fun getAllConfigs(): Flow<List<V2RayConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: V2RayConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllConfigs(configs: List<V2RayConfig>)

    @Update
    suspend fun updateConfig(config: V2RayConfig)

    @Delete
    suspend fun deleteConfig(config: V2RayConfig)

    @Query("DELETE FROM v2ray_configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)

    @Query("DELETE FROM v2ray_configs")
    suspend fun clearAllConfigsRaw()

    @Query("DELETE FROM v2ray_configs WHERE subscriptionName != 'سرور دستی'")
    suspend fun clearAllConfigs()

    @Query("UPDATE v2ray_configs SET latency = :latency WHERE id = :id")
    suspend fun updateConfigLatency(id: Long, latency: Long)

    @Query("UPDATE v2ray_configs SET optimizedIp = :ip WHERE id = :id")
    suspend fun updateConfigOptimizedIp(id: Long, ip: String)
}
