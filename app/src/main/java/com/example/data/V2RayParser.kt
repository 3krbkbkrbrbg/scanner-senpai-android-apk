package com.example.data

import android.util.Base64
import java.net.URLDecoder
import java.net.URLEncoder
import org.json.JSONObject

object V2RayParser {

    /**
     * Parses a list of lines retrieved from a subscription feed.
     * Decodes Base64 subscription payloads if the whole feed is encoded.
     */
    fun parseSubscriptionContent(content: String): List<V2RayConfig> {
        val configs = mutableListOf<V2RayConfig>()
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return configs

        var decodedContent: String? = null
        
        // Try cleaning and decoding with different Base64 flags
        val cleanCandidate = trimmed.replace("\n", "").replace("\r", "").replace(" ", "").trim()
        val decodingFlags = listOf(
            Base64.DEFAULT,
            Base64.URL_SAFE,
            Base64.NO_WRAP,
            Base64.NO_PADDING
        )

        for (flags in decodingFlags) {
            try {
                val decodedBytes = Base64.decode(cleanCandidate, flags)
                val testStr = String(decodedBytes, Charsets.UTF_8)
                if (testStr.contains("vmess://") || testStr.contains("vless://") || testStr.contains("trojan://") || testStr.contains("ss://")) {
                    decodedContent = testStr
                    break
                }
            } catch (e: Exception) {
                // Keep trying other flags
            }
        }

        // If no smart decode works, try raw candidate or raw trimmed
        if (decodedContent == null) {
            for (flags in decodingFlags) {
                try {
                    val decodedBytes = Base64.decode(trimmed, flags)
                    val testStr = String(decodedBytes, Charsets.UTF_8)
                    if (testStr.contains("vmess://") || testStr.contains("vless://") || testStr.contains("trojan://") || testStr.contains("ss://")) {
                        decodedContent = testStr
                        break
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // Fallback to original text if none contained proto markers but it was actually decoded
        if (decodedContent == null) {
            try {
                val decodedBytes = Base64.decode(cleanCandidate, Base64.DEFAULT)
                decodedContent = String(decodedBytes, Charsets.UTF_8)
            } catch (e: Exception) {
                decodedContent = content
            }
        }

        val lines = decodedContent!!.split("\n", "\r")
        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue
            val config = parseUri(trimmedLine)
            if (config != null) {
                configs.add(config)
            }
        }
        return configs
    }

    /**
     * Main dispatcher to parse single V2Ray URI formats.
     */
    fun parseUri(uri: String): V2RayConfig? {
        return try {
            when {
                uri.startsWith("vless://") -> parseVless(uri)
                uri.startsWith("vmess://") -> parseVmess(uri)
                uri.startsWith("trojan://") -> parseTrojan(uri)
                uri.startsWith("ss://") -> parseShadowsocks(uri)
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseVless(uri: String): V2RayConfig? {
        // vless://uuid@address:port?param=value#remark
        val cleanUri = uri.substring(8)
        val hashIndex = cleanUri.lastIndexOf('#')
        val remark = if (hashIndex != -1) {
            URLDecoder.decode(cleanUri.substring(hashIndex + 1), "UTF-8")
        } else {
            "VLESS-Config"
        }
        
        val mainSection = if (hashIndex != -1) cleanUri.substring(0, hashIndex) else cleanUri
        val parts = mainSection.split("@")
        if (parts.size != 2) return null
        
        val uuid = parts[0]
        val rest = parts[1]
        
        val qIndex = rest.indexOf('?')
        val addressPortPart = if (qIndex != -1) rest.substring(0, qIndex) else rest
        val queryPart = if (qIndex != -1) rest.substring(qIndex + 1) else ""
        
        val ap = addressPortPart.split(":")
        if (ap.isEmpty()) return null
        val address = ap[0]
        val port = if (ap.size > 1) ap[1].toIntOrNull() ?: 443 else 443
        
        val queryParams = parseQueryParams(queryPart)
        val security = queryParams["security"] ?: "none"
        val type = queryParams["type"] ?: "tcp"
        val host = queryParams["host"] ?: ""
        val path = queryParams["path"] ?: ""
        val sni = queryParams["sni"] ?: ""

        return V2RayConfig(
            remark = remark,
            protocol = "vless",
            address = address,
            port = port,
            uuid = uuid,
            security = security,
            type = type,
            host = host,
            path = path,
            sni = sni,
            rawUri = uri
        )
    }

    private fun parseTrojan(uri: String): V2RayConfig? {
        // trojan://password@address:port?param=value#remark
        val cleanUri = uri.substring(10)
        val hashIndex = cleanUri.lastIndexOf('#')
        val remark = if (hashIndex != -1) {
            URLDecoder.decode(cleanUri.substring(hashIndex + 1), "UTF-8")
        } else {
            "Trojan-Config"
        }
        
        val mainSection = if (hashIndex != -1) cleanUri.substring(0, hashIndex) else cleanUri
        val parts = mainSection.split("@")
        if (parts.size != 2) return null
        
        val password = parts[0]
        val rest = parts[1]
        
        val qIndex = rest.indexOf('?')
        val addressPortPart = if (qIndex != -1) rest.substring(0, qIndex) else rest
        val queryPart = if (qIndex != -1) rest.substring(qIndex + 1) else ""
        
        val ap = addressPortPart.split(":")
        if (ap.isEmpty()) return null
        val address = ap[0]
        val port = if (ap.size > 1) ap[1].toIntOrNull() ?: 443 else 443
        
        val queryParams = parseQueryParams(queryPart)
        val security = queryParams["security"] ?: "tls"
        val type = queryParams["type"] ?: "tcp"
        val host = queryParams["host"] ?: ""
        val path = queryParams["path"] ?: ""
        val sni = queryParams["sni"] ?: ""

        return V2RayConfig(
            remark = remark,
            protocol = "trojan",
            address = address,
            port = port,
            uuid = password,
            security = security,
            type = type,
            host = host,
            path = path,
            sni = sni,
            rawUri = uri
        )
    }

    private fun parseVmess(uri: String): V2RayConfig? {
        // vmess://base64_encoded_json
        val base64Part = uri.substring(8)
        val jsonStr = try {
            val decoded = Base64.decode(base64Part, Base64.DEFAULT)
            String(decoded, Charsets.UTF_8)
        } catch (e: Exception) {
            return null
        }
        
        val json = JSONObject(jsonStr)
        val remark = json.optString("ps", "VMess-Config")
        val address = json.optString("add", "")
        val port = json.optInt("port", 443)
        val uuid = json.optString("id", "")
        val security = json.optString("scy", "auto")
        val type = json.optString("net", "tcp")
        val host = json.optString("host", "")
        val path = json.optString("path", "")
        val sni = json.optString("sni", "")

        return V2RayConfig(
            remark = remark,
            protocol = "vmess",
            address = address,
            port = port,
            uuid = uuid,
            security = security,
            type = type,
            host = host,
            path = path,
            sni = sni,
            rawUri = uri
        )
    }

    private fun parseShadowsocks(uri: String): V2RayConfig? {
        // ss://base64_encoded@address:port#remark
        val cleanUri = uri.substring(5)
        val hashIndex = cleanUri.lastIndexOf('#')
        val remark = if (hashIndex != -1) {
            URLDecoder.decode(cleanUri.substring(hashIndex + 1), "UTF-8")
        } else {
            "SS-Config"
        }
        
        val mainSection = if (hashIndex != -1) cleanUri.substring(0, hashIndex) else cleanUri
        val parts = mainSection.split("@")
        
        var address = ""
        var port = 80
        var password = ""
        
        if (parts.size == 2) {
            // Standard format: ss://base64(method:password)@address:port
            val base64Creds = parts[0]
            val decodedCreds = try {
                String(Base64.decode(base64Creds, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                base64Creds
            }
            password = decodedCreds
            
            val ap = parts[1].split(":")
            address = ap[0]
            port = if (ap.size > 1) ap[1].toIntOrNull() ?: 8388 else 8388
        } else {
            // Legacys format: ss://base64(method:password@address:port)
            val decodedAll = try {
                String(Base64.decode(mainSection, Base64.DEFAULT), Charsets.UTF_8)
            } catch (e: Exception) {
                return null
            }
            val subdivide = decodedAll.split("@")
            if (subdivide.size != 2) return null
            password = subdivide[0]
            val ap = subdivide[1].split(":")
            address = ap[0]
            port = if (ap.size > 1) ap[1].toIntOrNull() ?: 8388 else 8388
        }

        return V2RayConfig(
            remark = remark,
            protocol = "shadowsocks",
            address = address,
            port = port,
            uuid = password, // Store credentials in password/uuid field
            rawUri = uri
        )
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        if (query.isEmpty()) return params
        try {
            val pairs = query.split("&")
            for (pair in pairs) {
                val idx = pair.indexOf("=")
                if (idx != -1) {
                    val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                    val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    params[key] = value
                }
            }
        } catch (e: Exception) {}
        return params
    }

    /**
     * Takes a custom dynamic IP and the parsed configuration, and reconstructs
     * an optimized/swapped V2Ray URI standard. Maintain Host/SNI.
     */
    fun optimizeUriWithIp(config: V2RayConfig, cleanIp: String): String {
        return try {
            when (config.protocol) {
                "vmess" -> {
                    // Update field of JSON and encode
                    val base64Part = config.rawUri.substring(8)
                    val decodedBytes = Base64.decode(base64Part, Base64.DEFAULT)
                    val json = JSONObject(String(decodedBytes, Charsets.UTF_8))
                    
                    // Set Host & SNI to the original address if they are blank 
                    val originalAddress = json.optString("add", config.address)
                    if (json.optString("host").isNullOrEmpty()) {
                        json.put("host", originalAddress)
                    }
                    if (json.optString("sni").isNullOrEmpty()) {
                        json.put("sni", originalAddress)
                    }
                    // Overwrite the address fields with clean Cloudflare IP
                    json.put("add", cleanIp)
                    
                    val newBase64 = Base64.encodeToString(
                        json.toString().toByteArray(Charsets.UTF_8), 
                        Base64.NO_WRAP or Base64.NO_PADDING
                    ).trim()
                    
                    "vmess://$newBase64"
                }
                "vless", "trojan" -> {
                    // Replace endpoint address parameter, and inject SNI/host
                    val sniParamValue = if (config.sni.isNotEmpty()) config.sni else config.address
                    val hostParamValue = if (config.host.isNotEmpty()) config.host else config.address
                    
                    val protocolPrefix = "${config.protocol}://"
                    val remarkPart = URLEncoder.encode(config.remark, "UTF-8")
                    
                    // Query Building
                    val queryBuilder = StringBuilder()
                    queryBuilder.append("security=${config.security}")
                    queryBuilder.append("&type=${config.type}")
                    queryBuilder.append("&sni=$sniParamValue")
                    queryBuilder.append("&host=$hostParamValue")
                    if (config.path.isNotEmpty()) {
                        queryBuilder.append("&path=${URLEncoder.encode(config.path, "UTF-8")}")
                    }
                    
                    "$protocolPrefix${config.uuid}@$cleanIp:${config.port}?$queryBuilder#$remarkPart"
                }
                "shadowsocks" -> {
                    // SS doesn't act easily inside standard HTTP routing but we can produce optimized URI
                    val remarkPart = URLEncoder.encode(config.remark, "UTF-8")
                    "ss://${config.uuid}@$cleanIp:${config.port}#$remarkPart"
                }
                else -> config.rawUri
            }
        } catch (e: Exception) {
            config.rawUri
        }
    }
}
