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

    private var activeScanJob: Job? = null

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
}
