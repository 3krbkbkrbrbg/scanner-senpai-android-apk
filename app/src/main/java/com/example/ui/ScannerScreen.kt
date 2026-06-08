package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe Scanner State flows
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val scannedCount by viewModel.scannedCount.collectAsStateWithLifecycle()
    val totalToScan by viewModel.totalToScan.collectAsStateWithLifecycle()
    val activeCleanIpsFound by viewModel.activeCleanIpsFound.collectAsStateWithLifecycle()
    val scannedIpsList by viewModel.scannedIpsList.collectAsStateWithLifecycle()
    val savedIpsList by viewModel.savedIpsList.collectAsStateWithLifecycle()

    // Observe V2Ray Config Flows
    val v2RayConfigs by viewModel.v2RayConfigsList.collectAsStateWithLifecycle()
    val selectedConfig by viewModel.selectedConfig.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptionsList.collectAsStateWithLifecycle()
    val isUpdatingSubs by viewModel.isUpdatingSubs.collectAsStateWithLifecycle()
    val isTestingConfigs by viewModel.isTestingConfigs.collectAsStateWithLifecycle()

    // Observe VPN state flows
    val vpnState by viewModel.vpnConnectionState.collectAsStateWithLifecycle()
    val vpnLogs by viewModel.vpnLogs.collectAsStateWithLifecycle()
    val uploadSpeed by viewModel.vpnUploadSpeed.collectAsStateWithLifecycle()
    val downloadSpeed by viewModel.vpnDownloadSpeed.collectAsStateWithLifecycle()
    val totalBytes by viewModel.vpnTotalBytes.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0=Scanner, 1=Saved IPs, 2=Config
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SenPaiSplashScreen(onTimeout = { showSplash = false })
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "SenPai Scanner Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "SenPai Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    "اسکنر حرفه‌ای آی‌پی تمیز کلودفلر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                actions = {
                    if (isScanning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Scanner Tab") },
                    label = { Text("اسکنر IP", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_scanner_tab")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved Tab") },
                    label = { Text("پین‌شده‌ها", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_saved_tab")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Config Tab") },
                    label = { Text("تنظیمات", fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_config_tab")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                        )
                    )
                )
        ) {
            // Scanner Stats Hero Bar (Only Visible when Scanning)
            AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "در حال اسکن آی‌پی‌ها... ($scannedCount / $totalToScan)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    "آی‌پی سالم یافت‌شده",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$activeCleanIpsFound IP",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Button(
                                onClick = { viewModel.stopScan() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("stop_scan_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Stop",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("توقف اسکن", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }

            when (activeTab) {
                0 -> ScannerView(
                    viewModel = viewModel,
                    isScanning = isScanning,
                    scannedIpsList = scannedIpsList,
                    onSaveIp = { ip, lat, speed ->
                        viewModel.saveIpToDb(ip, lat, speed)
                    },
                    onCopyIp = { ip ->
                        clipboardManager.setText(AnnotatedString(ip))
                        Toast.makeText(context, "آی‌پی $ip کپی شد", Toast.LENGTH_SHORT).show()
                    },
                    savedIps = savedIpsList
                )
                1 -> SavedResultsView(
                    viewModel = viewModel,
                    savedIps = savedIpsList,
                    onCopyIp = { ip ->
                        clipboardManager.setText(AnnotatedString(ip))
                        Toast.makeText(context, "آی‌پی کپی شد", Toast.LENGTH_SHORT).show()
                    },
                    onRemoveIp = { ip ->
                        viewModel.removeFromDb(ip)
                        Toast.makeText(context, "برداشته شد", Toast.LENGTH_SHORT).show()
                    }
                )
                2 -> ConfigView(viewModel = viewModel, isScanning = isScanning)
            }
        }
    }
}
}

@Composable
fun ScannerView(
    viewModel: ScannerViewModel,
    isScanning: Boolean,
    scannedIpsList: List<ScanResult>,
    onSaveIp: (String, Long, Double) -> Unit,
    onCopyIp: (String) -> Unit,
    savedIps: List<SavedIp>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (scannedIpsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Text(
                        text = "اسکنر خودکار کلودفلر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "این قسمت آی‌پی‌های تمیز و پرسرعت کلودفلر مخصوص متصل شدن به کلاینت و فیلتر شکن را شناسایی می‌کند. این آی‌پی‌های سالم را پس از اسکن می‌توانید در کانفیگ‌های وی‌پی‌ان خود استفاده کنید.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { viewModel.startScan() },
                        enabled = !isScanning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .testTag("start_scan_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Scan")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("شروع اسکن آی‌پی", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "لیست آی‌پی‌های کلودفلر یافت‌شده",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                if (!isScanning) {
                    Button(
                        onClick = { viewModel.startScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("restart_scan_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اسکن مجدد", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(scannedIpsList, key = { it.ip }) { item ->
                    ScanResultCard(
                        result = item,
                        isSaved = savedIps.any { it.ipAddress == item.ip },
                        onSave = { onSaveIp(item.ip, item.latency, item.downloadSpeed) },
                        onUnsave = { viewModel.removeByIpAddress(item.ip) },
                        onCopy = { onCopyIp(item.ip) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScanResultCard(
    result: ScanResult,
    isSaved: Boolean,
    onSave: () -> Unit,
    onUnsave: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.ip,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "پینگ: ${result.latency}ms",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (result.latency in 1..250) SlateAccentGreen else SlateAccentOrange
                    )
                    
                    if (result.downloadSpeed > 0.0) {
                        Text(
                            text = "سرعت: ${String.format("%.1f KB/s", result.downloadSpeed)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.Share, contentDescription = "Copy IP", modifier = Modifier.size(20.dp))
                }
                
                IconButton(onClick = { if (isSaved) onUnsave() else onSave() }) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Save IP",
                        tint = if (isSaved) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Subscriptions View
@Composable
fun SubscriptionsView(
    viewModel: ScannerViewModel,
    subscriptions: List<Subscription>,
    isUpdating: Boolean
) {
    val context = LocalContext.current
    var newSubName by remember { mutableStateOf("") }
    var newSubUrl by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "مخازن کانفیگ (v2ray Subscription)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "لینک ساب‌های خود را اضافه کنید. برنامه به طور خودکار به محض باز شدن مخازن را برای یافتن کانفیگ‌های تازه آپدیت می‌کند و پینگ می‌گیرد.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("افزودن مخزن جدید", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("نام تمایز مخرن (مثلاً مخزن رایگان)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = newSubUrl,
                    onValueChange = { newSubUrl = it },
                    label = { Text("لینک ساب (Subscription URL)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                Button(
                    onClick = {
                        if (newSubName.isNotBlank() && newSubUrl.isNotBlank()) {
                            viewModel.addSubscription(newSubName, newSubUrl)
                            newSubName = ""
                            newSubUrl = ""
                            Toast.makeText(context, "مخزن ساب اضافه و بروزرسانی شد!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "لطفاً مقادیر را پر کنید", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افزودن و بروزرسانی")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مخازن ثبت شده", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Button(
                onClick = { viewModel.updateAndPingAllSubscriptions() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                enabled = !isUpdating
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("آپدیت همگانی", style = MaterialTheme.typography.labelMedium)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(subscriptions, key = { it.id }) { sub ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sub.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                sub.url, 
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (sub.lastUpdated > 0) {
                                val date = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(sub.lastUpdated))
                                Text(
                                    "آخرین بروزرسانی: $date", 
                                    style = MaterialTheme.typography.labelSmall, 
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        
                        IconButton(onClick = { viewModel.deleteSubscription(sub) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Sub", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// V2Ray Configurations & Connection Center
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun V2RayClientView(
    viewModel: ScannerViewModel,
    v2RayConfigs: List<V2RayConfig>,
    selectedConfig: V2RayConfig?,
    savedIps: List<SavedIp>,
    vpnState: ConnectionState,
    logs: List<String>,
    uploadSpeed: Double,
    downloadSpeed: Double,
    totalBytes: Long,
    onCopyConfig: (V2RayConfig) -> Unit,
    onSwapIp: (V2RayConfig, String) -> Unit,
    onRevertIp: (V2RayConfig) -> Unit
) {
    val context = LocalContext.current
    var selectedConfigForOptimize by remember { mutableStateOf<V2RayConfig?>(null) }
    var showOptimizationDialog by remember { mutableStateOf(false) }
    var showAdvancedTools by remember { mutableStateOf(false) }
    val proxyTunnelAppsEnabled by viewModel.vpnService.proxyTunnelAppsEnabled.collectAsStateWithLifecycle()
    
    // Manual setup / Editing modes state
    var showEditDialog by remember { mutableStateOf(false) }
    var configToEdit by remember { mutableStateOf<V2RayConfig?>(null) }

    // Pulsing transition for connection animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Advanced Futuristic Core Control Dial (Inspired by Exclave style)
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp)
                .testTag("vpn_dashboard_card"),
            contentAlignment = Alignment.Center
        ) {
            // Pulse outer glowing radiant circles for active/handshaking connection states
            if (vpnState != ConnectionState.Idle) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer(
                            scaleX = pulseScale,
                            scaleY = pulseScale,
                            alpha = pulseAlpha
                        )
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                                colors = listOf(
                                    (if (vpnState == ConnectionState.Connected) SlateAccentGreen else SlateAccentOrange).copy(alpha = 0.35f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            // Continuous spinning ring when system is not Idle
            val spinAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "spin_angle"
            )
            
            androidx.compose.foundation.Canvas(modifier = Modifier.size(175.dp)) {
                // Subtle static background chassis ring
                drawCircle(
                    color = SlateSurfaceVariant.copy(alpha = 0.25f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5.dp.toPx())
                )
                
                // Active status reactive color indicator
                val strokeColor = when (vpnState) {
                    ConnectionState.Idle -> SlateTextSecondary.copy(alpha = 0.35f)
                    ConnectionState.Connected -> SlateAccentGreen
                    is ConnectionState.Error -> Color.Red
                    else -> SlatePrimary
                }
                
                // Rotate the glowing sectors around the core
                rotate(spinAngle) {
                    drawArc(
                        color = strokeColor,
                        startAngle = -90f,
                        sweepAngle = 100f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                    drawArc(
                        color = strokeColor.copy(alpha = 0.3f),
                        startAngle = 90f,
                        sweepAngle = 80f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            }
            
            // Beautiful interactive central tactical pad button
            Surface(
                onClick = {
                    if (vpnState == ConnectionState.Connected) {
                        viewModel.vpnService.disconnect()
                    } else {
                        val activeConfig = selectedConfig ?: v2RayConfigs.firstOrNull()
                        if (activeConfig != null) {
                            viewModel.vpnService.connect(activeConfig)
                        } else {
                            Toast.makeText(context, "لطفا ابتدا یک سرور اضافه کنید", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                shape = CircleShape,
                color = SlateSurface,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = when (vpnState) {
                        ConnectionState.Idle -> SlateSurfaceVariant
                        ConnectionState.Connected -> SlateAccentGreen
                        is ConnectionState.Error -> Color.Red
                        else -> SlatePrimary
                    }
                ),
                modifier = Modifier
                    .size(130.dp)
                    .testTag("vpn_toggle_button"),
                tonalElevation = 8.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = when (vpnState) {
                            ConnectionState.Idle -> Icons.Default.Lock
                            ConnectionState.Connected -> Icons.Default.CheckCircle
                            is ConnectionState.Error -> Icons.Default.Warning
                            else -> Icons.Default.Refresh
                        },
                        contentDescription = "Shield Connection Status",
                        tint = when (vpnState) {
                            ConnectionState.Idle -> SlateTextSecondary
                            ConnectionState.Connected -> SlateAccentGreen
                            is ConnectionState.Error -> Color.Red
                            else -> SlatePrimary
                        },
                        modifier = Modifier.size(28.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = when (vpnState) {
                            ConnectionState.Idle -> "غیرفعال"
                            ConnectionState.Connected -> "اتصال امن"
                            is ConnectionState.Error -> "خطای سرور"
                            ConnectionState.Resolving -> "تحلیل مسیر..."
                            ConnectionState.Handshaking -> "دست‌دهی..."
                            ConnectionState.Authenticating -> "رمزگذاری..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = when (vpnState) {
                            ConnectionState.Idle -> SlateTextSecondary
                            ConnectionState.Connected -> SlateAccentGreen
                            is ConnectionState.Error -> Color.Red
                            else -> SlatePrimary
                        }
                    )
                    
                    Text(
                        text = when (vpnState) {
                            ConnectionState.Idle -> "TAP TO ACTIVATE"
                            ConnectionState.Connected -> "SECURE / TOUCH TO RESET"
                            is ConnectionState.Error -> "RESTART CORRUPTION"
                            else -> "TUNNEL ESTABLISHING"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                        fontWeight = FontWeight.Bold,
                        color = SlateTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Active Server Display Indicator (Minimal and beautiful)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (vpnState == ConnectionState.Connected) SlateAccentGreen else SlateTextSecondary,
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = if (selectedConfig != null) "سرور فعال: ${selectedConfig.remark}" else "سروری انتخاب نشده است",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedConfig != null) SlateTextPrimary else SlateTextSecondary
                    )
                }
            }
        }

        Text(
            text = "لیست سرورها (${v2RayConfigs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        val groupedConfigs = remember(v2RayConfigs) {
            v2RayConfigs.groupBy { it.subscriptionName }
        }

        if (groupedConfigs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "هیچ کانفیگی یافت نشد. لطفاً در قسمت مخازن اقدام به آپدیت همگانی کنید یا یک سرور دستی بیفزایید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedConfigs.forEach { (subscriptionName, configsInSub) ->
                    // Subscription Category Sticky-style Header
                    item(key = "header_${subscriptionName}") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Subscription Folder",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = subscriptionName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${configsInSub.size} سرور",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Render items inside this subscription group
                    items(configsInSub, key = { it.id }) { config ->
                        val isSelected = selectedConfig?.id == config.id
                        val borderStroke = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectConfig(config)
                                    if (vpnState == ConnectionState.Connected) {
                                        viewModel.vpnService.connect(config)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = borderStroke
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                viewModel.selectConfig(config)
                                                if (vpnState == ConnectionState.Connected) {
                                                    viewModel.vpnService.connect(config)
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))

                                        val pillColor = when (config.protocol) {
                                            "vmess" -> MaterialTheme.colorScheme.primary
                                            "vless" -> MaterialTheme.colorScheme.secondary
                                            "trojan" -> SlateAccentOrange
                                            else -> Color.Gray
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(pillColor)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                config.protocol.uppercase(), 
                                                fontWeight = FontWeight.Bold, 
                                                color = Color.White, 
                                                fontSize = 9.sp
                                            )
                                        }
                                        
                                        Text(
                                            config.remark, 
                                            fontWeight = FontWeight.Bold, 
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val latText = if (config.latency > 0) "${config.latency} ms" else "Stalled"
                                        val latColor = if (config.latency > 0 && config.latency < 250) SlateAccentGreen else if (config.latency > 0) SlateAccentOrange else Color.Red
                                        Text(
                                            latText, 
                                            style = MaterialTheme.typography.labelSmall, 
                                            color = latColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "آدرس مقصد: ${config.address}:${config.port}", 
                                            style = MaterialTheme.typography.labelSmall, 
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (config.optimizedIp.isNotEmpty()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Check, 
                                                    contentDescription = "Optimized", 
                                                    tint = SlateAccentGreen,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    "Optimized: ${config.optimizedIp}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = SlateAccentGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (config.optimizedIp.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onRevertIp(config) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Refresh, 
                                                    contentDescription = "Revert Swap", 
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        
                                        Button(
                                            onClick = {
                                                selectedConfigForOptimize = config
                                                showOptimizationDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (config.optimizedIp.isNotEmpty()) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary
                                            ),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Build, 
                                                contentDescription = "Swap IP Address", 
                                                tint = if (config.optimizedIp.isNotEmpty()) MaterialTheme.colorScheme.secondary else Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                if (config.optimizedIp.isNotEmpty()) "ویرایش IP" else "تغییر IP", 
                                                fontSize = 9.sp,
                                                color = if (config.optimizedIp.isNotEmpty()) MaterialTheme.colorScheme.secondary else Color.White
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.pingSingleConfig(config) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow, 
                                                contentDescription = "Ping Single Server", 
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                configToEdit = config
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit, 
                                                contentDescription = "Edit Server Details", 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onCopyConfig(config) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Share, 
                                                contentDescription = "Copy Config URI", 
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.deleteConfig(config) },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete, 
                                                contentDescription = "Delete Server", 
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Advanced Section at the bottom of the main layout, below the list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Toggle row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAdvancedTools = !showAdvancedTools }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (showAdvancedTools) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = "Toggle Advanced Panel",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تنظیمات پیشرفته، ابزارها و کنسول",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SlateTextSecondary
                        )
                    }

                    if (!showAdvancedTools && vpnState == ConnectionState.Connected) {
                        Text(
                            text = "▼ ${String.format("%.1f", downloadSpeed)} KB/s  ▲ ${String.format("%.1f", uploadSpeed)} KB/s",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = SlateAccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showAdvancedTools) {
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), thickness = 1.dp)

                    // 1. Toolbars for imports and pings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                configToEdit = null // Adding mode
                                showEditDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Config", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("افزودن دستی", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }

                        val clipboardManager = LocalClipboardManager.current
                        Button(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text ?: ""
                                viewModel.importFromClipboard(
                                    text = clipText,
                                    onSuccess = { count ->
                                        Toast.makeText(context, "$count سرور با موفقیت وارد شد!", Toast.LENGTH_LONG).show()
                                    },
                                    onError = { errorMsg ->
                                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            modifier = Modifier.weight(1.2f).testTag("clipboard_import_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Paste Clipboard", modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("افزودن از کلیپ‌بورد", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }

                        Button(
                            onClick = { viewModel.pingAllConfigs() },
                            enabled = !viewModel.isTestingConfigs.collectAsStateWithLifecycle().value,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Test Ping", modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("پینگ همگانی", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        }
                    }

                    // 2. Metrics Telemetry Cards (Gauges)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Download Icon", tint = SlateAccentGreen, modifier = Modifier.size(14.dp))
                                    Text("دریافت (Download)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (vpnState == ConnectionState.Connected) String.format("%.1f KB/s", downloadSpeed) else "0.0 KB/s",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SlateAccentGreen
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, "Upload Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Text("ارسال (Upload)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (vpnState == ConnectionState.Connected) String.format("%.1f KB/s", uploadSpeed) else "0.0 KB/s",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 3. Proxy application list system toggle card
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("proxy_tunnel_apps_card"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Proxy Apps Icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Column {
                                    Text("پراکسی کردن سایر اپلیکیشن‌ها", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                                    Text("هدایت ترافیک مرورگرها و پیام‌رسان‌ها (مثل تلگرام)", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary, fontSize = 8.sp)
                                }
                            }
                            Switch(
                                checked = proxyTunnelAppsEnabled,
                                onCheckedChange = { viewModel.vpnService.setProxyTunnelAppsEnabled(it) },
                                modifier = Modifier.testTag("proxy_tunnel_apps_switch")
                            )
                        }
                    }

                    // 4. Debug Config checks details
                    DebugLogSection(
                        vpnState = vpnState,
                        logs = logs,
                        totalBytes = totalBytes,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 5. Terminal log simulator window
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF02040B))
                            .padding(6.dp)
                    ) {
                        val displayLogs = logs.takeLast(3)
                        if (displayLogs.isEmpty()) {
                            Text(
                                "کنسول سیستم آماده تانلینگ...",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                color = SlateTextSecondary
                            )
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(displayLogs) { logLine ->
                                    Text(
                                        logLine,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = if (logLine.contains("successfully") || logLine.contains("Connected") || logLine.contains("system")) SlateAccentGreen else if (logLine.contains("Error") || logLine.contains("Fatal")) MaterialTheme.colorScheme.error else SlateTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOptimizationDialog && selectedConfigForOptimize != null) {
        AlertDialog(
            onDismissRequest = { showOptimizationDialog = false },
            title = { Text("جایگزینی آی‌پی معتبر کلودفلر") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "آی‌پی مورد نظر جهت تزریق به کانفیگ را از میان آی‌پی‌های پرسرعتی که با موفقیت اسکن کرده‌اید انتخاب کنید:",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (savedIps.isEmpty()) {
                        Text(
                            "آی‌پی ذخیره شده در دیتابیس ندارید. لطفاً به زبانه اسکنر بروید و پس از اسکن، آی‌پی‌های سالم را ستاره‌دار کنید تا اینجا مشاهده شود.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(savedIps) { ipNode ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            onSwapIp(selectedConfigForOptimize!!, ipNode.ipAddress)
                                            showOptimizationDialog = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        ipNode.ipAddress, 
                                        fontFamily = FontFamily.Monospace, 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "${ipNode.latency} ms | ${String.format("%.1f KB/s", ipNode.downloadSpeed)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOptimizationDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    if (showEditDialog) {
        EditConfigDialog(
            config = configToEdit,
            onDismiss = { showEditDialog = false },
            onSave = { updatedOrNew ->
                if (updatedOrNew.id == 0L) {
                    viewModel.addManualConfig(updatedOrNew)
                } else {
                    viewModel.updateConfig(updatedOrNew)
                }
                showEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditConfigDialog(
    config: V2RayConfig?,
    onDismiss: () -> Unit,
    onSave: (V2RayConfig) -> Unit
) {
    var remark by remember { mutableStateOf(config?.remark ?: "") }
    var protocol by remember { mutableStateOf(config?.protocol ?: "vless") }
    var address by remember { mutableStateOf(config?.address ?: "") }
    var port by remember { mutableStateOf(config?.port?.toString() ?: "443") }
    var uuid by remember { mutableStateOf(config?.uuid ?: "") }
    var security by remember { mutableStateOf(config?.security ?: "none") }
    var type by remember { mutableStateOf(config?.type ?: "tcp") }
    var host by remember { mutableStateOf(config?.host ?: "") }
    var path by remember { mutableStateOf(config?.path ?: "") }
    var sni by remember { mutableStateOf(config?.sni ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = if (config == null) "افزودن کانفیگ دستی" else "ویرایش کانفیگ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { remark = it },
                        label = { Text("نام سرور / Remark") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Column {
                        Text("پروتکل اتصال:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            listOf("vless", "vmess", "trojan", "shadowsocks").forEach { prot ->
                                FilterChip(
                                    selected = protocol == prot,
                                    onClick = { protocol = prot },
                                    label = { Text(prot.uppercase()) }
                                )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("آدرس سرور / IP / Domain") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("پورت / Port") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        label = { Text(if (protocol == "shadowsocks" || protocol == "trojan") "رمز عبور / Password" else "شناسه / UUID") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (protocol != "shadowsocks") {
                    item {
                        OutlinedTextField(
                            value = security,
                            onValueChange = { security = it },
                            label = { Text("امنیت (Security: e.g. none, tls)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("شبکه (Network Type: tcp, ws, grpc)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("هاست / Host Header") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = path,
                            onValueChange = { path = it },
                            label = { Text("مسیر / Path") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = sni,
                            onValueChange = { sni = it },
                            label = { Text("اس ان آی / SNI") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (remark.isBlank() || address.isBlank() || port.isBlank()) return@Button
                    val pNum = port.toIntOrNull() ?: 443
                    val newOrUpdated = V2RayConfig(
                        id = config?.id ?: 0,
                        remark = remark,
                        protocol = protocol,
                        address = address,
                        port = pNum,
                        uuid = uuid,
                        security = security,
                        type = type,
                        host = host,
                        path = path,
                        sni = sni,
                        rawUri = config?.rawUri ?: "$protocol://$uuid@$address:$port",
                        optimizedIp = config?.optimizedIp ?: "",
                        latency = config?.latency ?: -1,
                        timestamp = config?.timestamp ?: System.currentTimeMillis()
                    )
                    onSave(newOrUpdated)
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
fun SavedResultsView(
    viewModel: ScannerViewModel,
    savedIps: List<SavedIp>,
    onCopyIp: (String) -> Unit,
    onRemoveIp: (SavedIp) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "آی‌پی‌های پین‌شده‌ی شما (${savedIps.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (savedIps.isNotEmpty()) {
                TextButton(
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    onClick = { viewModel.clearHistory() }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("کامل پاک کن", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        if (savedIps.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "هیچ آی‌پی ذخیره شده‌ای یافت نشد. پس از اسکن، آی‌پی‌های سالم را ستاره‌دار کنید.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedIps, key = { it.id }) { node ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    node.ipAddress,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "پینگ: ${node.latency}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SlateAccentGreen
                                    )
                                    if (node.downloadSpeed > 0) {
                                        Text(
                                            "سرعت: ${String.format("%.1f KB/s", node.downloadSpeed)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Row {
                                IconButton(onClick = { onCopyIp(node.ipAddress) }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Copy IP")
                                }
                                IconButton(onClick = { onRemoveIp(node) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigView(
    viewModel: ScannerViewModel,
    isScanning: Boolean
) {
    val port by viewModel.port.collectAsStateWithLifecycle()
    val ipCount by viewModel.ipCount.collectAsStateWithLifecycle()
    val threadsCount by viewModel.threadsCount.collectAsStateWithLifecycle()
    val timeoutMs by viewModel.timeoutMs.collectAsStateWithLifecycle()
    val testSpeedEnabled by viewModel.testSpeedEnabled.collectAsStateWithLifecycle()
    val customIpCidr by viewModel.customIpCidr.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("پارامترهای اسکن آی‌پی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "این تنظیمات نحوه پویش و جستجوی پینگ آی‌پی‌های لبه کلودفلر را کنترل می‌کند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("پورت هدف اسکن", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = port.toString(),
                        onValueChange = { viewModel.port.value = it.toIntOrNull() ?: 443 },
                        label = { Text("پورت (به طور پیشفرض 443)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعداد آی‌پی‌های تولیدی جهت اسکن", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$ipCount IP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = ipCount.toFloat(),
                        onValueChange = { viewModel.ipCount.value = it.toInt() },
                        valueRange = 10f..500f,
                        enabled = !isScanning
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تعداد ترد همزمان (Threads)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$threadsCount ترد", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = threadsCount.toFloat(),
                        onValueChange = { viewModel.threadsCount.value = it.toInt() },
                        valueRange = 1f..64f,
                        enabled = !isScanning
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("حداکثر مهلت پاسخ (Timeout)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("$timeoutMs ms", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = timeoutMs.toFloat(),
                        onValueChange = { viewModel.timeoutMs.value = it.toInt() },
                        valueRange = 300f..3000f,
                        enabled = !isScanning
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("اسکن تست سرعت دانلود", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("پس از یافتن آی‌پی پینگ مناسب، یک دانلود ۵۰۰ کیلوبایتی جهت کنترل صحت پهنای باند انجام می‌دهد.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = testSpeedEnabled,
                        onCheckedChange = { viewModel.testSpeedEnabled.value = it },
                        enabled = !isScanning
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("رنج آی‌پی اختصاصی (CIDR block)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = customIpCidr,
                        onValueChange = { viewModel.customIpCidr.value = it },
                        placeholder = { Text("مثال: 162.159.192.0/24") },
                        label = { Text("رنج آی‌پی جداگانه (خالی برای آی‌پی رندوم)") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning
                    )
                }
            }
        }

        // About SenPai Scanner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlatePrimary.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SlatePrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = SlatePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                "SenPai Scanner Suite",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = SlateTextPrimary
                            )
                            Text(
                                "ابزار فوق‌حرفه‌ای تست کیفیت و یافتن IP تمیز کلودفلر",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateAccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Checked", tint = SlateAccentGreen, modifier = Modifier.size(16.dp))
                            Text("تولید خودکار رنج‌های گسترده IP جهت پوشش حداکثری", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Checked", tint = SlateAccentGreen, modifier = Modifier.size(16.dp))
                            Text("پینگ همزمان و چندرشته‌ای (Multi-Threaded) با کارایی بالا", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Checked", tint = SlateAccentGreen, modifier = Modifier.size(16.dp))
                            Text("اندازه‌گیری دقیق پینگ، جیتر و فیلترینگ شبکه", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, "Checked", tint = SlateAccentGreen, modifier = Modifier.size(16.dp))
                            Text("تست سرعت واقعی دانلود و پهنای باند آی‌پی‌ها", style = MaterialTheme.typography.bodySmall, color = SlateTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "نسخه برنامه: v3.1.0 SenPai Edition\nتوسعه یافته با ❤️ برای آزادی اینترنت و سهولت کاربران ایرانی.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateTextSecondary.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SenPaiSplashScreen(
    onTimeout: () -> Unit
) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040611)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rounded Search Frame with SenPai Primary border
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF0D1226),
                border = androidx.compose.foundation.BorderStroke(2.dp, SlatePrimary.copy(alpha = 0.8f)),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "SenPai Logo",
                        tint = SlateAccentGreen,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SenPai Scanner",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp
                ),
                color = SlateTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "اسکنر هوشمند و ارزیابی کیفیت IP کلودفلر",
                style = MaterialTheme.typography.bodySmall,
                color = SlateAccentGreen,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                color = SlatePrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun TrafficIndicator(uploadSpeed: Double, downloadSpeed: Double, isConnected: Boolean) {
    if (!isConnected) return
    
    val formatSpeed: (Double) -> String = { speed ->
        if (speed >= 1024) {
            String.format("%.1f MB/s", speed / 1024.0)
        } else {
            String.format("%.1f KB/s", speed)
        }
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(end = 4.dp, start = 4.dp)
            .testTag("traffic_indicator_badge"),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Downloads",
                    tint = SlateAccentGreen,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "↓ ${formatSpeed(downloadSpeed)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Uploads",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "↑ ${formatSpeed(uploadSpeed)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DebugLogSection(
    vpnState: ConnectionState,
    logs: List<String>,
    totalBytes: Long,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var logFilter by remember { mutableStateOf("ALL") }

    Card(
        modifier = modifier.fillMaxWidth().testTag("debug_log_section_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Status Logs",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "کنسول دیباگ و وضعیت تانل (Debug Console)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (vpnState == ConnectionState.Connected) SlateAccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                ) {
                    Text(
                        text = if (vpnState == ConnectionState.Connected) "Active (tun0)" else "Offline",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (vpnState == ConnectionState.Connected) SlateAccentGreen else SlateTextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF030712), RoundedCornerShape(8.dp)).padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tunnel Status:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        Text(
                            text = when (vpnState) {
                                ConnectionState.Idle -> "Inactive / Idle"
                                ConnectionState.Resolving -> "DNS & Edge Path Resolving"
                                ConnectionState.Handshaking -> "TCP Link handshake active"
                                ConnectionState.Authenticating -> "Verifying credential payload"
                                ConnectionState.Connected -> "TUN connected & multiplex overlay active"
                                is ConnectionState.Error -> "Tunnel fault error block"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (vpnState == ConnectionState.Connected) SlateAccentGreen else if (vpnState is ConnectionState.Error) MaterialTheme.colorScheme.error else SlateTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active DNS Routers:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        Text("1.1.1.1 (Cloudflare IPv4), 8.8.8.8 (Google)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Active Interfaces & IP:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        Text("tun0 (IPv4: 10.0.0.2/32, IPv6: fd00:a::2/128)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Forwarding Gateway Routes:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        Text("0.0.0.0/0 (IPv4 Default), ::/0 (IPv6 Default)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = SlateTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NAT & Protection:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        Text("Full NAT, Protect Socket loops active", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Medium, color = SlateTextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Relayed Volume:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SlateTextSecondary)
                        val totalMB = totalBytes / (1024.0 * 1024.0)
                        val packetsCount = totalBytes / 1200
                        Text(
                            text = String.format("%.2f MB (%d packets intercepted)", totalMB, packetsCount),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("فیلتر لاگ:", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                    listOf("ALL", "INFO", "WARNING", "ERROR").forEach { level ->
                        val isSelected = logFilter == level
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.clickable { logFilter = level }
                        ) {
                            Text(
                                text = level,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = if (isSelected) Color.White else SlateTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF040713), RoundedCornerShape(6.dp))
                        .padding(6.dp)
                ) {
                    val filteredLogs = remember(logs, logFilter) {
                        logs.filter { line ->
                            when (logFilter) {
                                "INFO" -> !line.contains("warning", ignoreCase = true) && !line.contains("error", ignoreCase = true) && !line.contains("failed", ignoreCase = true) && !line.contains("fatal", ignoreCase = true)
                                "WARNING" -> line.contains("warning", ignoreCase = true) || line.contains("pending", ignoreCase = true)
                                "ERROR" -> line.contains("error", ignoreCase = true) || line.contains("fatal", ignoreCase = true) || line.contains("failed", ignoreCase = true)
                                else -> true
                            }
                        }
                    }

                    if (filteredLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لاگی در این فیلتر وجود ندارد", style = MaterialTheme.typography.labelSmall, color = SlateTextSecondary)
                        }
                    } else {
                        val state = rememberLazyListState()
                        LaunchedEffect(filteredLogs.size) {
                            if (filteredLogs.isNotEmpty()) {
                                state.animateScrollToItem(filteredLogs.size - 1)
                            }
                        }
                        
                        LazyColumn(
                            state = state,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(filteredLogs) { line ->
                                val color = when {
                                    line.contains("warning", ignoreCase = true) -> SlateAccentOrange
                                    line.contains("error", ignoreCase = true) || line.contains("failed", ignoreCase = true) || line.contains("fatal", ignoreCase = true) -> MaterialTheme.colorScheme.error
                                    line.contains("successfully", ignoreCase = true) || line.contains("connected", ignoreCase = true) -> SlateAccentGreen
                                    else -> SlateTextPrimary
                                }
                                Text(
                                    text = line,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = color,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

