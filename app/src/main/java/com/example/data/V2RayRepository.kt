package com.example.data

import kotlinx.coroutines.flow.Flow

class V2RayRepository(private val v2RayDao: V2RayDao) {

    val allSubscriptions: Flow<List<Subscription>> = v2RayDao.getAllSubscriptions()
    val allConfigs: Flow<List<V2RayConfig>> = v2RayDao.getAllConfigs()

    suspend fun insertSubscription(sub: Subscription) {
        v2RayDao.insertSubscription(sub)
    }

    suspend fun deleteSubscription(sub: Subscription) {
        v2RayDao.deleteSubscription(sub)
    }

    suspend fun getSubscriptionsList(): List<Subscription> {
        return v2RayDao.getSubscriptionsList()
    }

    suspend fun updateSubscriptionTimestamp(id: Long, timestamp: Long) {
        v2RayDao.updateSubTimestamp(id, timestamp)
    }

    suspend fun insertConfig(config: V2RayConfig) {
        v2RayDao.insertConfig(config)
    }

    suspend fun insertAllConfigs(configs: List<V2RayConfig>) {
        v2RayDao.insertAllConfigs(configs)
    }

    suspend fun updateConfig(config: V2RayConfig) {
        v2RayDao.updateConfig(config)
    }

    suspend fun deleteConfig(config: V2RayConfig) {
        v2RayDao.deleteConfig(config)
    }

    suspend fun deleteConfigById(id: Long) {
        v2RayDao.deleteConfigById(id)
    }

    suspend fun clearAllConfigs() {
        v2RayDao.clearAllConfigs()
    }

    suspend fun updateConfigLatency(id: Long, latency: Long) {
        v2RayDao.updateConfigLatency(id, latency)
    }

    suspend fun updateConfigOptimizedIp(id: Long, ip: String) {
        v2RayDao.updateConfigOptimizedIp(id, ip)
    }
}
