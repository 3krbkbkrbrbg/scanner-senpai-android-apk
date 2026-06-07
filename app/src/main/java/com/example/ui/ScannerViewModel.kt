package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = IpRepository(db.savedIpDao)
    val v2RayRepo = V2RayRepository(db.v2RayDao)
    val vpnService = VpnServiceSimulator(application)
    private val scanEngine = ScanEngine()

    // Configuration Settings State
    val port = MutableStateFlow(443)
    val ipCount = MutableStateFlow(100)
    val threadsCount = MutableStateFlow(16)
    val timeoutMs = MutableStateFlow(1200)
    val testSpeedEnabled = MutableStateFlow(true)
    val customIpCidr = MutableStateFlow("") 

    // Scan Stats
    val isScanning = MutableStateFlow(false)
    val progress = MutableStateFlow(0f)
    val scannedCount = MutableStateFlow(0)
    val totalToScan = MutableStateFlow(0)
    val activeCleanIpsFound = MutableStateFlow(0)

    // Subscriptions & V2Ray states
    val isUpdatingSubs = MutableStateFlow(false)
    val isTestingConfigs = MutableStateFlow(false)

    val subscriptionsList: StateFlow<List<Subscription>> = v2RayRepo.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val v2RayConfigsList: StateFlow<List<V2RayConfig>> = v2RayRepo.allConfigs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val selectedConfig = MutableStateFlow<V2RayConfig?>(null)

    // Live scanner progress outcomes
    private val _scannedIpsList = MutableStateFlow<List<ScanResult>>(emptyList())
    val scannedIpsList: StateFlow<List<ScanResult>> = _scannedIpsList.asStateFlow()

    // Saved logs in DB
    val savedIpsList: StateFlow<List<SavedIp>> = repository.allSavedIps
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // VPN Tunnel Simulator flows
    val vpnConnectionState = vpnService.connectionState
    val vpnLogs = vpnService.logs
    val vpnUploadSpeed = vpnService.uploadSpeed
    val vpnDownloadSpeed = vpnService.downloadSpeed
    val vpnTotalBytes = vpnService.totalBytes

    private var activeScanJob: Job? = null

    init {
        // Pre-populate default subscription feed if DB is empty
        viewModelScope.launch {
            v2RayRepo.allSubscriptions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    v2RayRepo.insertSubscription(
                        Subscription(
                            name = "Iboxz Free Collector",
                            url = "https://raw.githubusercontent.com/iboxz/free-v2ray-collector/main/sub"
                        )
                    )
                }
            }
            // Trigger auto updating of repositories and ping tests on launch
            updateAndPingAllSubscriptions()
        }

        // Synchronize selected config with list updates
        viewModelScope.launch {
            v2RayConfigsList.collect { list ->
                val current = selectedConfig.value
                if (current == null) {
                    if (list.isNotEmpty()) {
                        selectedConfig.value = list.first()
                    }
                } else {
                    val updated = list.find { it.id == current.id }
                    if (updated != null) {
                        selectedConfig.value = updated
                    } else if (list.isNotEmpty()) {
                        selectedConfig.value = list.first()
                    } else {
                        selectedConfig.value = null
                    }
                }
            }
        }
    }

    fun selectConfig(config: V2RayConfig) {
        selectedConfig.value = config
    }

    fun addManualConfig(config: V2RayConfig) {
        viewModelScope.launch {
            v2RayRepo.insertConfig(config)
        }
    }

    fun updateConfig(config: V2RayConfig) {
        viewModelScope.launch {
            v2RayRepo.updateConfig(config)
            if (selectedConfig.value?.id == config.id) {
                selectedConfig.value = config
            }
        }
    }

    fun deleteConfig(config: V2RayConfig) {
        viewModelScope.launch {
            v2RayRepo.deleteConfig(config)
        }
    }

    fun pingSingleConfig(config: V2RayConfig) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val hostToPing = if (config.optimizedIp.isNotEmpty()) config.optimizedIp else config.address
            val latency = try {
                val socket = java.net.Socket()
                val start = System.currentTimeMillis()
                socket.connect(java.net.InetSocketAddress(hostToPing, config.port), 1500)
                val duration = System.currentTimeMillis() - start
                socket.close()
                duration
            } catch (e: Exception) {
                -1L
            }
            v2RayRepo.updateConfigLatency(config.id, latency)
        }
    }

    /**
     * Download V2Ray configuration subscriptions via HTTP, parse them,
     * save them to the DB, and automatically coordinate latency testing.
     */
    fun updateAndPingAllSubscriptions() {
        if (isUpdatingSubs.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isUpdatingSubs.value = true
            try {
                val subList = v2RayRepo.getSubscriptionsList()
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                
                val parsedConfigs = mutableListOf<V2RayConfig>()
                
                for (sub in subList) {
                    if (!sub.isActive) continue
                    try {
                        val request = okhttp3.Request.Builder()
                            .url(sub.url)
                            .header("User-Agent", "v2rayNG/1.8.5")
                            .build()
                        
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string() ?: ""
                                val decodedConfigs = V2RayParser.parseSubscriptionContent(body).map {
                                    it.copy(subscriptionName = sub.name)
                                }
                                parsedConfigs.addAll(decodedConfigs)
                                v2RayRepo.updateSubscriptionTimestamp(sub.id, System.currentTimeMillis())
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                if (parsedConfigs.isNotEmpty()) {
                    v2RayRepo.clearAllConfigs()
                    // Filter down to supported protocols and store in db
                    v2RayRepo.insertAllConfigs(parsedConfigs.take(150)) // cap to 150 clean entries
                }
                
                // Automatically run latency pings
                pingAllConfigs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isUpdatingSubs.value = false
            }
        }
    }

    /**
     * Ping test all downloaded configurations in parallel.
     */
    fun pingAllConfigs() {
        if (isTestingConfigs.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            isTestingConfigs.value = true
            try {
                val configs = v2RayRepo.allConfigs.first()
                if (configs.isEmpty()) return@launch
                
                val semaphore = Semaphore(16) // Max 16 concurrent socket tests
                val jobs = configs.map { config ->
                    launch {
                        semaphore.withPermit {
                            val latency = try {
                                val socket = java.net.Socket()
                                val start = System.currentTimeMillis()
                                val hostToPing = if (config.optimizedIp.isNotEmpty()) config.optimizedIp else config.address
                                socket.connect(java.net.InetSocketAddress(hostToPing, config.port), 1500)
                                val duration = System.currentTimeMillis() - start
                                socket.close()
                                duration
                            } catch (e: Exception) {
                                -1L
                            }
                            v2RayRepo.updateConfigLatency(config.id, latency)
                        }
                    }
                }
                jobs.forEach { it.join() }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isTestingConfigs.value = false
            }
        }
    }

    /**
     * Manually add a custom subscription feed
     */
    fun addSubscription(name: String, url: String) {
        viewModelScope.launch {
            v2RayRepo.insertSubscription(Subscription(name = name, url = url))
            updateAndPingAllSubscriptions()
        }
    }

    /**
     * Delete custom subscription feed and its items
     */
    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            v2RayRepo.deleteSubscription(sub)
            updateAndPingAllSubscriptions()
        }
    }

    /**
     * Optimizes a V2Ray Config by setting a clean measured Cloudflare IP address
     */
    fun optimizeConfigWithCleanIp(config: V2RayConfig, ipAddress: String) {
        viewModelScope.launch {
            v2RayRepo.updateConfigOptimizedIp(config.id, ipAddress)
            // Re-run ping on this config to measure speed over optimized IP
            launch(kotlinx.coroutines.Dispatchers.IO) {
                val latency = try {
                    val socket = java.net.Socket()
                    val start = System.currentTimeMillis()
                    socket.connect(java.net.InetSocketAddress(ipAddress, config.port), 1500)
                    val duration = System.currentTimeMillis() - start
                    socket.close()
                    duration
                } catch (e: Exception) {
                    -1L
                }
                v2RayRepo.updateConfigLatency(config.id, latency)
            }
        }
    }

    /**
     * Reverts custom optimization
     */
    fun removeOptimization(config: V2RayConfig) {
        viewModelScope.launch {
            v2RayRepo.updateConfigOptimizedIp(config.id, "")
            launch(kotlinx.coroutines.Dispatchers.IO) {
                val latency = try {
                    val socket = java.net.Socket()
                    val start = System.currentTimeMillis()
                    socket.connect(java.net.InetSocketAddress(config.address, config.port), 1500)
                    val duration = System.currentTimeMillis() - start
                    socket.close()
                    duration
                } catch (e: Exception) {
                    -1L
                }
                v2RayRepo.updateConfigLatency(config.id, latency)
            }
        }
    }

    fun startScan() {
        if (isScanning.value) return
        
        activeScanJob = viewModelScope.launch {
            isScanning.value = true
            scannedCount.value = 0
            progress.value = 0f
            _scannedIpsList.value = emptyList()
            activeCleanIpsFound.value = 0

            val currentPort = port.value
            val currentTimeout = timeoutMs.value
            val runSpeedTest = testSpeedEnabled.value
            val currentThreads = threadsCount.value

            // Generate targeted IP pool
            val generatedIps = if (customIpCidr.value.isNotBlank()) {
                val parsed = CidrParser.parseCidr(customIpCidr.value.trim())
                if (parsed != null) {
                    val countToGet = minOf(ipCount.value, parsed.size)
                    (0 until countToGet).map { parsed.getIpAt(it) }
                } else {
                    CidrParser.generateSampleIps(ipCount.value)
                }
            } else {
                CidrParser.generateSampleIps(ipCount.value)
            }

            totalToScan.value = generatedIps.size
            if (generatedIps.isEmpty()) {
                isScanning.value = false
                return@launch
            }

            val semaphore = Semaphore(currentThreads)
            val tempResultsList = mutableListOf<ScanResult>()
            val scannedAtomic = AtomicInteger(0)
            val cleanAtomic = AtomicInteger(0)

            val jobs = generatedIps.map { ip ->
                launch {
                    semaphore.withPermit {
                        if (!isScanning.value) return@withPermit 

                        var res = scanEngine.checkLatency(ip, currentPort, currentTimeout)
                        if (res.isClean) {
                            cleanAtomic.incrementAndGet()
                            activeCleanIpsFound.value = cleanAtomic.get()

                            if (runSpeedTest && isScanning.value) {
                                // Run high-speed connection test to Cloudflare
                                val speed = scanEngine.checkDownloadSpeed(ip, 500000, 3000)
                                res = res.copy(downloadSpeed = speed)
                            }

                            synchronized(tempResultsList) {
                                tempResultsList.add(res)
                                // Sort descending by cleanest/fastest speed first, then lowest latency
                                _scannedIpsList.value = tempResultsList.sortedWith(
                                    compareByDescending<ScanResult> { it.downloadSpeed }
                                        .thenBy { it.latency }
                                )
                            }
                        }

                        val completed = scannedAtomic.incrementAndGet()
                        scannedCount.value = completed
                        progress.value = completed.toFloat() / generatedIps.size.toFloat()
                    }
                }
            }

            jobs.forEach { it.join() }
            isScanning.value = false
        }
    }

    fun stopScan() {
        if (!isScanning.value) return
        activeScanJob?.cancel()
        isScanning.value = false
    }

    fun saveIpToDb(ipAddress: String, latency: Long, downloadSpeed: Double) {
        viewModelScope.launch {
            repository.saveIp(
                SavedIp(
                    ipAddress = ipAddress,
                    port = port.value,
                    latency = latency,
                    downloadSpeed = downloadSpeed
                )
            )
        }
    }

    fun removeFromDb(savedIp: SavedIp) {
        viewModelScope.launch {
            repository.removeIp(savedIp)
        }
    }

    fun removeByIpAddress(ipAddress: String) {
        viewModelScope.launch {
            repository.removeByIp(ipAddress)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun importFromClipboard(text: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        if (text.isBlank()) {
            onError("کلیپ‌بورد خالی است!")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val parsed = V2RayParser.parseSubscriptionContent(text).map {
                    it.copy(subscriptionName = "سرور دستی")
                }
                if (parsed.isEmpty()) {
                    onError("هیچ کانفیگ معتبری یافت نشد! کانفیگ‌های vless، vmess، trojan یا ss معتبر کپی کنید.")
                } else {
                    v2RayRepo.insertAllConfigs(parsed)
                    onSuccess(parsed.size)
                }
            } catch (e: Exception) {
                onError("خطا در پردازش اطلاعات کلیپ‌بورد: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        vpnService.disconnect()
    }
}
