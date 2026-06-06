package com.example.data

import kotlinx.coroutines.flow.Flow

class IpRepository(private val savedIpDao: SavedIpDao) {
    val allSavedIps: Flow<List<SavedIp>> = savedIpDao.getAllSavedIps()

    suspend fun saveIp(ip: SavedIp) {
        savedIpDao.insertIp(ip)
    }

    suspend fun removeIp(ip: SavedIp) {
        savedIpDao.deleteIp(ip)
    }

    suspend fun removeByIp(ipAddress: String) {
        savedIpDao.deleteByIp(ipAddress)
    }

    suspend fun clearHistory() {
        savedIpDao.clearAll()
    }
}
