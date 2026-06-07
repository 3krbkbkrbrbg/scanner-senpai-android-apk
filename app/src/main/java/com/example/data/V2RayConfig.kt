package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "v2ray_configs")
data class V2RayConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remark: String,
    val protocol: String, // "vmess", "vless", "trojan", "shadowsocks"
    val address: String, // original target IP or domain
    val port: Int,
    val uuid: String, // uuid or password
    val security: String = "none",
    val type: String = "tcp", // network type (ws, grpc, tcp)
    val host: String = "", // host header (usually sni or cdn domain)
    val path: String = "", // ws or grpc path
    val sni: String = "", // SNI for SSL handshake
    val rawUri: String, // the original imported URI link
    val optimizedIp: String = "", // swapped Cloudflare IP if optimized
    val latency: Long = -1, // latency in milliseconds (-1 if not tested)
    val isConnected: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val subscriptionName: String = "سرور دستی"
)
