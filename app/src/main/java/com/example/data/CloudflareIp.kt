package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_ips")
data class SavedIp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ipAddress: String,
    val port: Int,
    val latency: Long, // in ms
    val downloadSpeed: Double, // in KB/s (0.0 if not tested)
    val timestamp: Long = System.currentTimeMillis()
)
