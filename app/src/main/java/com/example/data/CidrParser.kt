package com.example.data

object CidrParser {
    val CLOUDFLARE_IPV4_CIDRS = listOf(
        "173.245.48.0/20",
        "103.21.244.0/22",
        "103.22.200.0/22",
        "103.31.4.0/22",
        "141.101.64.0/18",
        "108.162.192.0/18",
        "190.93.240.0/20",
        "188.114.96.0/20",
        "197.234.240.0/22",
        "198.41.128.0/17",
        "162.158.0.0/15",
        "104.16.0.0/13",
        "104.24.0.0/14",
        "172.64.0.0/13",
        "131.0.72.0/22"
    )

    fun parseCidr(cidr: String): IpRange? {
        return try {
            val parts = cidr.split("/")
            if (parts.size != 2) return null
            val ipPart = parts[0]
            val maskPart = parts[1].toInt()
            
            val ipBytes = ipPart.split(".")
            if (ipBytes.size != 4) return null
            
            var ipInt = 0
            for (i in 0..3) {
                ipInt = ipInt or (ipBytes[i].toInt() shl (24 - i * 8))
            }
            
            val mask = if (maskPart == 0) 0 else (-1 shl (32 - maskPart))
            val network = ipInt and mask
            val size = if (32 - maskPart >= 30) {
                // Return a maximum safe constraint size for generating random indexes
                10000
            } else {
                1 shl (32 - maskPart)
            }
            
            IpRange(network, size)
        } catch (e: Exception) {
            null
        }
    }

    class IpRange(val network: Int, val size: Int) {
        fun getIpAt(index: Int): String {
            val ipInt = network + index
            return decodeIp(ipInt)
        }

        fun getRandomIp(): String {
            if (size <= 1) return decodeIp(network)
            val randomIndex = (0 until size).random()
            return getIpAt(randomIndex)
        }
    }

    fun decodeIp(ipInt: Int): String {
        return "${(ipInt ushr 24) and 0xFF}.${(ipInt ushr 16) and 0xFF}.${(ipInt ushr 8) and 0xFF}.${ipInt and 0xFF}"
    }

    /**
     * Generates a unique sample list of randomly selected IPs from across Cloudflare blocks.
     */
    fun generateSampleIps(count: Int): List<String> {
        val list = mutableSetOf<String>()
        val parsedRanges = CLOUDFLARE_IPV4_CIDRS.mapNotNull { parseCidr(it) }
        if (parsedRanges.isEmpty()) return emptyList()

        var attempts = 0
        // Sample until we get the count, with an emergency breakout at 5 * count attempts
        while (list.size < count && attempts < count * 5) {
            attempts++
            val range = parsedRanges.random()
            list.add(range.getRandomIp())
        }
        return list.toList()
    }
}
