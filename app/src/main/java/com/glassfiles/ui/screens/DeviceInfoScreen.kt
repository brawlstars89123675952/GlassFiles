package com.glassfiles.ui.screens

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.*
import android.telephony.TelephonyManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

data class InfoItem(val label: String, val value: String)

@Composable
fun DeviceInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val deviceItems = remember { getDeviceInfo() }
    val osItems = remember { getOsInfo() }
    val cpuItems = remember { getCpuInfo() }
    val gpuItems = remember { getGpuInfo() }
    val ramItems = remember { getRamInfo(context) }
    val storageItems = remember { getStorageInfo() }
    val batteryItems = remember { getBatteryInfo(context) }
    val thermalItems = remember { getThermalInfo() }
    val displayItems = remember { getDisplayInfo(context) }
    val networkItems = remember { getNetworkInfo(context) }
    val sensorItems = remember { getSensorInfo(context) }
    val cameraItems = remember { getCameraInfo(context) }
    val cameraBlocks = remember { getCameraBlocks(context) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        Row(Modifier.fillMaxWidth().background(SurfaceWhite).padding(top = 44.dp, start = 4.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(20.dp), tint = Blue) }
            Text(Strings.deviceInfo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = {
                val sb = StringBuilder()
                fun add(t: String, items: List<InfoItem>) { sb.appendLine("=== $t ==="); items.forEach { sb.appendLine("${it.label}: ${it.value}") }; sb.appendLine() }
                add(Strings.deviceSection, deviceItems); add("OS", osItems); add(Strings.cpuSection, cpuItems)
                add("GPU", gpuItems); add(Strings.ramSection, ramItems); add(Strings.storageSection, storageItems)
                add(Strings.batterySection, batteryItems); add(Strings.displaySection, displayItems)
                add(Strings.networkSection, networkItems); add(Strings.sensorsSection, sensorItems)
                cameraBlocks.forEach { add(it.title, it.items) }
                (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Device Info", sb))
                Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
            }) { Icon(Icons.Rounded.ContentCopy, Strings.copyAll, Modifier.size(20.dp), tint = Blue) }
        }

        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoSection(Strings.deviceSection, Icons.Rounded.PhoneAndroid, Color(0xFF607D8B), deviceItems)
            InfoSection("OS", Icons.Rounded.Android, Color(0xFF4CAF50), osItems)
            InfoSection(Strings.cpuSection, Icons.Rounded.Memory, Color(0xFFE91E63), cpuItems)
            InfoSection("GPU", Icons.Rounded.Gradient, Color(0xFF673AB7), gpuItems)
            InfoSection(Strings.ramSection, Icons.Rounded.Storage, Color(0xFF9C27B0), ramItems)
            InfoSection(Strings.storageSection, Icons.Rounded.SdCard, Color(0xFF4CAF50), storageItems)
            InfoSection(Strings.batterySection, Icons.Rounded.BatteryFull, Color(0xFFFF9800), batteryItems)
            InfoSection(s("Термальные зоны", "Thermal Zones"), Icons.Rounded.Thermostat, Color(0xFFFF5722), thermalItems)
            InfoSection(Strings.displaySection, Icons.Rounded.Smartphone, Color(0xFF2196F3), displayItems)
            InfoSection(Strings.networkSection, Icons.Rounded.Wifi, Color(0xFF00BCD4), networkItems)
            InfoSection(Strings.sensorsSection, Icons.Rounded.Sensors, Color(0xFFFF5722), sensorItems)
            // Per-camera blocks
            cameraBlocks.forEachIndexed { i, block ->
                val icon = when (block.facing) {
                    "front" -> Icons.Rounded.CameraFront
                    "tof" -> Icons.Rounded.Sensors
                    else -> Icons.Rounded.CameraAlt
                }
                val color = when (block.facing) {
                    "front" -> Color(0xFF9C27B0)
                    "tof" -> Color(0xFF607D8B)
                    else -> Color(0xFF795548)
                }
                InfoSection(block.title, icon, color, block.items)
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun s(ru: String, en: String) = if (Strings.lang == com.glassfiles.data.AppLanguage.RUSSIAN) ru else en

@Composable
private fun InfoSection(title: String, icon: ImageVector, color: Color, items: List<InfoItem>) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (expanded) 0f else -90f, tween(250), label = "rot")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SurfaceWhite)) {
        Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(32.dp).background(color.copy(0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(18.dp), tint = color) }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text("${items.size}", fontSize = 12.sp, color = TextTertiary)
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(20.dp).rotate(rotation), tint = TextTertiary)
        }
        AnimatedVisibility(expanded, enter = expandVertically(tween(300)) + fadeIn(tween(200)), exit = shrinkVertically(tween(250)) + fadeOut(tween(150))) {
            Column {
                items.forEachIndexed { i, item ->
                    if (i > 0) Box(Modifier.fillMaxWidth().padding(start = 60.dp).height(0.5.dp).background(SeparatorColor))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.label, fontSize = 14.sp, color = TextSecondary, modifier = Modifier.weight(0.4f))
                        Text(item.value, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace, modifier = Modifier.weight(0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ═══════════════════════════════════
// Device
// ═══════════════════════════════════
private fun getDeviceInfo(): List<InfoItem> {
    val items = mutableListOf(
        InfoItem(Strings.model, Build.MODEL),
        InfoItem(Strings.manufacturer, Build.MANUFACTURER),
        InfoItem(s("Бренд", "Brand"), Build.BRAND),
        InfoItem(Strings.board, Build.BOARD),
        InfoItem(Strings.hardware, Build.HARDWARE),
        InfoItem(s("Устройство", "Device"), Build.DEVICE),
        InfoItem(s("Продукт", "Product"), Build.PRODUCT),
    )
    if (Build.VERSION.SDK_INT >= 31) {
        val soc = Build.SOC_MODEL
        val mfr = Build.SOC_MANUFACTURER
        if (soc != "unknown") items.add(1, InfoItem(s("Чипсет", "Chipset"), "$mfr $soc"))
    }
    items += InfoItem(Strings.bootloader, Build.BOOTLOADER)
    items += InfoItem("Serial", try { Build.getSerial() } catch (_: Exception) { "N/A" })
    items += InfoItem("Fingerprint", Build.FINGERPRINT.takeLast(50))
    return items
}

// ═══════════════════════════════════
// OS
// ═══════════════════════════════════
private fun getOsInfo(): List<InfoItem> {
    val items = mutableListOf(
        InfoItem("Android", Build.VERSION.RELEASE),
        InfoItem(Strings.apiLevel, "${Build.VERSION.SDK_INT}"),
        InfoItem(Strings.buildNumber, Build.DISPLAY),
        InfoItem("Build ID", Build.ID),
        InfoItem("Build Type", Build.TYPE),
        InfoItem("Build Tags", Build.TAGS),
        InfoItem("Codename", Build.VERSION.CODENAME),
        InfoItem(Strings.securityPatch, if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "N/A"),
        InfoItem(s("Ядро Linux", "Linux Kernel"), System.getProperty("os.version") ?: "N/A"),
        InfoItem(s("Язык", "Language"), java.util.Locale.getDefault().displayLanguage),
        InfoItem(s("Регион", "Region"), java.util.Locale.getDefault().displayCountry),
        InfoItem(s("Часовой пояс", "Timezone"), java.util.TimeZone.getDefault().id),
        InfoItem("Uptime", formatUptime(SystemClock.elapsedRealtime())),
    )
    // Java VM
    items += InfoItem("Java VM", "${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
    return items
}

private fun formatUptime(ms: Long): String {
    val s = ms / 1000; val d = s / 86400; val h = (s % 86400) / 3600; val m = (s % 3600) / 60
    return if (d > 0) "${d}d ${h}h ${m}m" else "${h}h ${m}m"
}

// ═══════════════════════════════════
// CPU — scan ALL cores
// ═══════════════════════════════════
private fun getCpuInfo(): List<InfoItem> {
    val items = mutableListOf<InfoItem>()

    // SoC name (API 31+)
    if (Build.VERSION.SDK_INT >= 31) {
        val soc = Build.SOC_MODEL; val mfr = Build.SOC_MANUFACTURER
        if (soc != "unknown" && soc.isNotBlank()) items += InfoItem(s("Чипсет", "Chipset"), "$mfr $soc")
    }

    // Hardware from /proc/cpuinfo
    try {
        val cpuInfo = File("/proc/cpuinfo").readText()
        cpuInfo.lines().find { it.startsWith("Hardware") }?.substringAfter(":")?.trim()?.let {
            items += InfoItem(Strings.hardware, it)
        }
        cpuInfo.lines().find { it.startsWith("model name") }?.substringAfter(":")?.trim()?.let {
            items += InfoItem(s("Модель ядра", "Core Model"), it)
        }
        val bogomips = cpuInfo.lines().find { it.startsWith("BogoMIPS") }?.substringAfter(":")?.trim()
        if (bogomips != null) items += InfoItem("BogoMIPS", bogomips)
        val revision = cpuInfo.lines().find { it.startsWith("CPU revision") }?.substringAfter(":")?.trim()
        if (revision != null) items += InfoItem(s("Ревизия", "Revision"), revision)
        val variant = cpuInfo.lines().find { it.startsWith("CPU variant") }?.substringAfter(":")?.trim()
        if (variant != null) items += InfoItem(s("Вариант", "Variant"), variant)
        val implementer = cpuInfo.lines().find { it.startsWith("CPU implementer") }?.substringAfter(":")?.trim()
        if (implementer != null) items += InfoItem(s("Разработчик", "Implementer"), implementer)
        val part = cpuInfo.lines().find { it.startsWith("CPU part") }?.substringAfter(":")?.trim()
        if (part != null) items += InfoItem(s("Часть", "Part"), part)
    } catch (_: Exception) {}

    val numCores = Runtime.getRuntime().availableProcessors()
    items += InfoItem(Strings.cores, "$numCores")
    items += InfoItem(Strings.architecture, Build.SUPPORTED_ABIS.joinToString(", "))
    items += InfoItem(s("32-бит ABI", "32-bit ABIs"), (Build.SUPPORTED_32_BIT_ABIS ?: emptyArray()).joinToString(", ").ifEmpty { "N/A" })
    items += InfoItem(s("64-бит ABI", "64-bit ABIs"), (Build.SUPPORTED_64_BIT_ABIS ?: emptyArray()).joinToString(", ").ifEmpty { "N/A" })

    // Scan ALL cores for min/max + clusters
    var globalMin = Long.MAX_VALUE; var globalMax = 0L
    val clusterMap = mutableMapOf<Long, MutableList<Int>>()

    for (i in 0 until numCores) {
        try {
            val minF = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq").readText().trim().toLong()
            val maxF = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
            if (minF < globalMin) globalMin = minF
            if (maxF > globalMax) globalMax = maxF
            clusterMap.getOrPut(maxF) { mutableListOf() }.add(i)
        } catch (_: Exception) {}
    }

    if (globalMax > 0) {
        items += InfoItem(Strings.cpuFreqMin, "${globalMin / 1000} MHz")
        items += InfoItem(Strings.cpuFreqMax, "${globalMax / 1000} MHz (${"%.2f".format(globalMax / 1_000_000.0)} GHz)")

        // Clusters
        val clusters = clusterMap.entries.sortedByDescending { it.key }
        clusters.forEachIndexed { idx, (freq, cores) ->
            val label = when {
                clusters.size <= 1 -> "Cluster"
                idx == 0 -> s("Прайм", "Prime")
                idx == clusters.lastIndex -> s("Эффективные", "Efficiency")
                else -> s("Производительные", "Performance")
            }
            items += InfoItem("$label (${cores.size}×)", "${freq / 1000} MHz")
        }
    }

    // Current freq per core
    val curFreqs = (0 until numCores).mapNotNull { i ->
        try { File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq").readText().trim().toLong() / 1000 } catch (_: Exception) { null }
    }
    if (curFreqs.isNotEmpty()) items += InfoItem(s("Текущие частоты", "Current Freqs"), curFreqs.joinToString(", ") { "${it}M" })

    // Governor
    try {
        val gov = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
        items += InfoItem(Strings.cpuGovernor, gov)
        val avail = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors").readText().trim()
        items += InfoItem(s("Доступные регуляторы", "Available Governors"), avail)
    } catch (_: Exception) {}

    return items
}

// ═══════════════════════════════════
// GPU
// ═══════════════════════════════════
private fun getGpuInfo(): List<InfoItem> {
    val items = mutableListOf<InfoItem>()
    try {
        val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val ver = IntArray(2)
        EGL14.eglInitialize(eglDisplay, ver, 0, ver, 1)
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)
        val surfAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        val surface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfAttribs, 0)
        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        val eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, surface, surface, eglContext)

        val renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "N/A"
        val vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "N/A"
        val glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: "N/A"
        val extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
        val slVersion = GLES20.glGetString(GLES20.GL_SHADING_LANGUAGE_VERSION) ?: "N/A"

        items += InfoItem(s("Рендерер", "Renderer"), renderer)
        items += InfoItem(s("Производитель", "Vendor"), vendor)
        items += InfoItem("OpenGL ES", glVersion)
        items += InfoItem("GLSL", slVersion)

        // Max texture size
        val maxTex = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        items += InfoItem(s("Макс. текстура", "Max Texture"), "${maxTex[0]}×${maxTex[0]}")

        val maxVP = IntArray(2)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VIEWPORT_DIMS, maxVP, 0)
        items += InfoItem(s("Макс. viewport", "Max Viewport"), "${maxVP[0]}×${maxVP[1]}")

        val maxVaryings = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VARYING_VECTORS, maxVaryings, 0)
        items += InfoItem("Max Varying Vectors", "${maxVaryings[0]}")

        val maxFragUniforms = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_FRAGMENT_UNIFORM_VECTORS, maxFragUniforms, 0)
        items += InfoItem("Max Fragment Uniforms", "${maxFragUniforms[0]}")

        val maxVertUniforms = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_UNIFORM_VECTORS, maxVertUniforms, 0)
        items += InfoItem("Max Vertex Uniforms", "${maxVertUniforms[0]}")

        val maxVertAttribs = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_VERTEX_ATTRIBS, maxVertAttribs, 0)
        items += InfoItem("Max Vertex Attribs", "${maxVertAttribs[0]}")

        val extCount = extensions.split(" ").filter { it.isNotBlank() }.size
        items += InfoItem(s("Расширения", "Extensions"), "$extCount")

        // Vulkan check
        val hasVulkan = try { File("/dev/kgsl-3d0").exists() || File("/system/lib64/libvulkan.so").exists() } catch (_: Exception) { false }
        items += InfoItem("Vulkan", if (hasVulkan) s("Поддерживается", "Supported") else "N/A")

        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglDestroySurface(eglDisplay, surface)
        EGL14.eglTerminate(eglDisplay)
    } catch (e: Exception) {
        items += InfoItem(s("Ошибка", "Error"), e.message ?: "N/A")
    }
    return items
}

// ═══════════════════════════════════
// RAM
// ═══════════════════════════════════
private fun getRamInfo(context: Context): List<InfoItem> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val mi = ActivityManager.MemoryInfo(); am.getMemoryInfo(mi)
    val total = mi.totalMem; val avail = mi.availMem; val used = total - avail
    val pct = (used.toDouble() / total * 100).toInt()
    val items = mutableListOf(
        InfoItem(Strings.totalRam, fmtBytes(total)),
        InfoItem(Strings.availableRam, fmtBytes(avail)),
        InfoItem(Strings.usedRam, "${fmtBytes(used)} ($pct%)"),
        InfoItem(s("Низкая память", "Low Memory"), if (mi.lowMemory) s("Да", "Yes") else s("Нет", "No")),
        InfoItem(s("Порог", "Threshold"), fmtBytes(mi.threshold))
    )
    // zRAM
    try {
        val zramSize = File("/sys/block/zram0/disksize").readText().trim().toLong()
        if (zramSize > 0) items += InfoItem("zRAM", fmtBytes(zramSize))
    } catch (_: Exception) {}
    // Swap
    try {
        val meminfo = File("/proc/meminfo").readText()
        val swapTotal = meminfo.lines().find { it.startsWith("SwapTotal") }?.replace(Regex("[^0-9]"), "")?.toLongOrNull()
        val swapFree = meminfo.lines().find { it.startsWith("SwapFree") }?.replace(Regex("[^0-9]"), "")?.toLongOrNull()
        if (swapTotal != null && swapTotal > 0) {
            items += InfoItem("Swap", "${fmtBytes(swapTotal * 1024)} / ${fmtBytes((swapFree ?: 0) * 1024)} ${s("свободно", "free")}")
        }
    } catch (_: Exception) {}
    return items
}

// ═══════════════════════════════════
// Storage
// ═══════════════════════════════════
private fun getStorageInfo(): List<InfoItem> {
    val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
    val total = stat.totalBytes; val free = stat.availableBytes; val used = total - free
    val items = mutableListOf(
        InfoItem(Strings.totalStorage, fmtBytes(total)),
        InfoItem(Strings.usedStorage, "${fmtBytes(used)} (${(used * 100 / total).toInt()}%)"),
        InfoItem(Strings.freeStorage, fmtBytes(free)),
        InfoItem(s("Размер блока", "Block Size"), "${stat.blockSizeLong} B"),
        InfoItem(s("Всего блоков", "Total Blocks"), "${stat.blockCountLong}")
    )
    // Check /data
    try {
        val dataStat = StatFs("/data")
        items += InfoItem("/data", "${fmtBytes(dataStat.totalBytes)} (${fmtBytes(dataStat.availableBytes)} ${s("свободно", "free")})")
    } catch (_: Exception) {}
    // External SD
    val ext = System.getenv("SECONDARY_STORAGE")
    if (ext != null && File(ext).exists()) {
        try {
            val sdStat = StatFs(ext)
            items += InfoItem("SD Card", "${fmtBytes(sdStat.totalBytes)} (${fmtBytes(sdStat.availableBytes)} ${s("свободно", "free")})")
        } catch (_: Exception) {}
    }
    return items
}

// ═══════════════════════════════════
// Battery
// ═══════════════════════════════════
private fun getBatteryInfo(context: Context): List<InfoItem> {
    val batt = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val level = batt?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batt?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
    val pct = if (scale > 0) level * 100 / scale else -1
    val status = when (batt?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> Strings.charging
        BatteryManager.BATTERY_STATUS_DISCHARGING -> Strings.discharging
        BatteryManager.BATTERY_STATUS_FULL -> Strings.full
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> s("Не заряжается", "Not charging")
        else -> Strings.unknown
    }
    val health = when (batt?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> Strings.healthGood
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> Strings.healthOverheat
        BatteryManager.BATTERY_HEALTH_DEAD -> Strings.healthDead
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> s("Перенапряжение", "Over Voltage")
        BatteryManager.BATTERY_HEALTH_COLD -> s("Холодная", "Cold")
        else -> Strings.unknown
    }
    val plugged = when (batt?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> s("Беспроводная", "Wireless")
        else -> s("Нет", "None")
    }
    val temp = (batt?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    val voltage = (batt?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0) / 1000.0
    val tech = batt?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: Strings.unknown

    val items = mutableListOf(
        InfoItem(Strings.batteryLevel, "$pct%"),
        InfoItem(Strings.batteryStatus, status),
        InfoItem(s("Зарядка от", "Plugged"), plugged),
        InfoItem(Strings.batteryHealth, health),
        InfoItem(Strings.batteryTemp, "${"%.1f".format(temp)}°C"),
        InfoItem(Strings.batteryVoltage, "${"%.3f".format(voltage)} V"),
        InfoItem(Strings.batteryTech, tech)
    )

    // BatteryManager properties
    try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (currentNow != 0 && currentNow != Int.MIN_VALUE) items += InfoItem(s("Ток", "Current"), "${currentNow / 1000} mA")
        val currentAvg = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        if (currentAvg != 0 && currentAvg != Int.MIN_VALUE) items += InfoItem(s("Средний ток", "Avg Current"), "${currentAvg / 1000} mA")
        val chargeCounter = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        if (chargeCounter > 0) items += InfoItem(s("Заряд", "Charge"), "${chargeCounter / 1000} mAh")
        val energyCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        if (energyCounter > 0) items += InfoItem(s("Энергия", "Energy"), "${energyCounter / 1_000_000} mWh")
    } catch (_: Exception) {}

    // Read sysfs for detailed battery info
    val psDir = File("/sys/class/power_supply/")
    val battDir = psDir.listFiles()?.find { d ->
        try { File(d, "type").readText().trim().equals("Battery", ignoreCase = true) } catch (_: Exception) { false }
    }
    if (battDir != null) {
        fun readSys(name: String): String? = try { File(battDir, name).readText().trim().takeIf { it.isNotBlank() && it != "0" } } catch (_: Exception) { null }

        readSys("charge_full")?.toLongOrNull()?.let { items += InfoItem(s("Ёмкость (текущая)", "Capacity (current)"), "${it / 1000} mAh") }
        readSys("charge_full_design")?.toLongOrNull()?.let { items += InfoItem(s("Ёмкость (заводская)", "Capacity (design)"), "${it / 1000} mAh") }

        // Battery wear
        val full = readSys("charge_full")?.toLongOrNull()
        val design = readSys("charge_full_design")?.toLongOrNull()
        if (full != null && design != null && design > 0) {
            val wear = ((1.0 - full.toDouble() / design) * 100)
            items += InfoItem(s("Износ", "Wear"), "${"%.1f".format(wear)}%")
            items += InfoItem(s("Здоровье батареи", "Battery Health"), "${"%.1f".format(full.toDouble() / design * 100)}%")
        }

        readSys("cycle_count")?.let { if (it != "0") items += InfoItem(s("Циклы зарядки", "Charge Cycles"), it) }
        readSys("charge_counter")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Счётчик заряда", "Charge Counter"), "${it / 1000} mAh") }
        readSys("power_now")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Мощность", "Power"), "${"%.1f".format(it / 1_000_000.0)} W") }
        readSys("voltage_max")?.toLongOrNull()?.let { items += InfoItem(s("Макс. напряжение", "Max Voltage"), "${"%.3f".format(it / 1_000_000.0)} V") }
        readSys("voltage_min")?.toLongOrNull()?.let { items += InfoItem(s("Мин. напряжение", "Min Voltage"), "${"%.3f".format(it / 1_000_000.0)} V") }
        readSys("current_max")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Макс. ток", "Max Current"), "${it / 1000} mA") }
        readSys("input_current_limit")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Лимит тока", "Current Limit"), "${it / 1000} mA") }
        readSys("charge_type")?.let { items += InfoItem(s("Тип зарядки", "Charge Type"), it) }
        readSys("time_to_full_now")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("До полного", "Time to Full"), "${it / 60} ${s("мин", "min")}") }
        readSys("time_to_empty_avg")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("До разряда", "Time to Empty"), "${it / 60} ${s("мин", "min")}") }
        readSys("manufacturer")?.let { items += InfoItem(s("Производитель АКБ", "Battery Mfr"), it) }
        readSys("model_name")?.let { items += InfoItem(s("Модель АКБ", "Battery Model"), it) }
        readSys("serial_number")?.let { items += InfoItem(s("Серийный АКБ", "Battery Serial"), it) }
        readSys("technology")?.let { if (it != tech) items += InfoItem(s("Технология (sysfs)", "Technology (sysfs)"), it) }
    }

    // Charger info
    val chargerDir = psDir.listFiles()?.find { d ->
        try {
            val type = File(d, "type").readText().trim()
            type.equals("USB", ignoreCase = true) || type.equals("Mains", ignoreCase = true)
        } catch (_: Exception) { false }
    }
    if (chargerDir != null) {
        fun readCh(name: String): String? = try { File(chargerDir, name).readText().trim().takeIf { it.isNotBlank() } } catch (_: Exception) { null }
        readCh("voltage_max")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Зарядка: напряжение", "Charger Voltage"), "${"%.1f".format(it / 1_000_000.0)} V") }
        readCh("current_max")?.toLongOrNull()?.let { if (it > 0) items += InfoItem(s("Зарядка: ток", "Charger Current"), "${it / 1000} mA") }
        val power = (chargerDir.let { d ->
            val v = try { File(d, "voltage_max").readText().trim().toLong() } catch (_: Exception) { 0L }
            val c = try { File(d, "current_max").readText().trim().toLong() } catch (_: Exception) { 0L }
            v.toDouble() * c / 1_000_000_000_000.0
        })
        if (power > 0.5) items += InfoItem(s("Зарядка: мощность", "Charger Power"), "${"%.1f".format(power)} W")
    }

    return items
}

// ═══════════════════════════════════
// Thermal
// ═══════════════════════════════════
private fun getThermalInfo(): List<InfoItem> {
    val items = mutableListOf<InfoItem>()
    try {
        val thermalDir = File("/sys/class/thermal/")
        if (thermalDir.exists()) {
            thermalDir.listFiles()?.filter { it.name.startsWith("thermal_zone") }?.sortedBy {
                it.name.replace("thermal_zone", "").toIntOrNull() ?: 0
            }?.forEach { zone ->
                val type = try { File(zone, "type").readText().trim() } catch (_: Exception) { zone.name }
                val temp = try { File(zone, "temp").readText().trim().toLong() } catch (_: Exception) { null }
                if (temp != null && temp > 0) {
                    val tempC = if (temp > 1000) temp / 1000.0 else temp.toDouble()
                    items += InfoItem(type, "${"%.1f".format(tempC)}°C")
                }
            }
        }
    } catch (_: Exception) {}
    return items
}

// ═══════════════════════════════════
// Display
// ═══════════════════════════════════
private fun getDisplayInfo(context: Context): List<InfoItem> {
    val dm = context.resources.displayMetrics
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager

    // Get display
    val display = if (Build.VERSION.SDK_INT >= 30) context.display
        else @Suppress("DEPRECATION") wm.defaultDisplay

    // Native resolution from Display.Mode (highest supported mode)
    var nativeW = 0; var nativeH = 0; var maxRefresh = 60f
    val allRefreshRates = mutableSetOf<Int>()
    try {
        val modes = display?.supportedModes
        if (modes != null && modes.isNotEmpty()) {
            // Find mode with highest resolution
            val bestMode = modes.maxByOrNull { it.physicalWidth * it.physicalHeight }
            if (bestMode != null) { nativeW = bestMode.physicalWidth; nativeH = bestMode.physicalHeight }
            // Find max refresh rate across all modes
            maxRefresh = modes.maxOf { it.refreshRate }
            modes.forEach { allRefreshRates.add(it.refreshRate.toInt()) }
        }
    } catch (_: Exception) {}

    // Current resolution (may be lower if user set FHD+ instead of WQHD+)
    val wPx: Int; val hPx: Int
    if (Build.VERSION.SDK_INT >= 30) {
        val bounds = wm.currentWindowMetrics.bounds
        wPx = bounds.width(); hPx = bounds.height()
    } else {
        val realSize = android.graphics.Point()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealSize(realSize)
        wPx = realSize.x; hPx = realSize.y
    }

    // Use native resolution if available, else current
    val dispW = if (nativeW > wPx) nativeW else wPx
    val dispH = if (nativeH > hPx) nativeH else hPx

    val currentRefresh = try { "%.0f".format(display?.refreshRate ?: 60f) } catch (_: Exception) { "60" }

    val items = mutableListOf(
        InfoItem(s("Нативное разрешение", "Native Resolution"), "${dispW} × ${dispH}"),
    )
    if (dispW != wPx || dispH != hPx) {
        items += InfoItem(s("Текущее разрешение", "Current Resolution"), "${wPx} × ${hPx}")
    }
    items += InfoItem(Strings.screenSize, "—")
    items += InfoItem(Strings.density, "${dm.densityDpi} dpi (${dm.density}x)")
    items += InfoItem("xdpi / ydpi", "${"%.1f".format(dm.xdpi)} / ${"%.1f".format(dm.ydpi)}")
    items += InfoItem(s("Макс. частота", "Max Refresh Rate"), "${"%.0f".format(maxRefresh)} Hz")
    items += InfoItem(s("Текущая частота", "Current Refresh Rate"), "$currentRefresh Hz")
    if (allRefreshRates.size > 1) {
        items += InfoItem(s("Доступные частоты", "Available Rates"), allRefreshRates.sorted().joinToString(", ") { "${it} Hz" })
    }
    items += InfoItem(s("Масштаб шрифтов", "Font Scale"), "${context.resources.configuration.fontScale}x")
    items += InfoItem(s("Ориентация", "Orientation"), if (context.resources.configuration.orientation == 1) s("Портрет", "Portrait") else s("Альбом", "Landscape"))
    items += InfoItem(s("Тёмная тема", "Dark Mode"), if ((context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES) s("Да", "Yes") else s("Нет", "No"))
    return items
}







// ═══════════════════════════════════
// Network
// ═══════════════════════════════════
private fun getNetworkInfo(context: Context): List<InfoItem> {
    val items = mutableListOf<InfoItem>()
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val caps = cm.getNetworkCapabilities(cm.activeNetwork)
    val transport = when {
        caps == null -> s("Нет", "None")
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> s("Сотовая", "Cellular")
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> Strings.unknown
    }
    items += InfoItem(Strings.networkType, transport)
    items += InfoItem(Strings.batteryStatus, if (caps != null) Strings.connected else Strings.disconnected)

    // WiFi
    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION") val info = wm.connectionInfo
            items += InfoItem(Strings.wifiNetwork, info.ssid?.replace("\"", "") ?: "N/A")
            items += InfoItem(s("Скорость", "Speed"), "${info.linkSpeed} Mbps")
            items += InfoItem(s("Частота", "Frequency"), "${info.frequency} MHz")
            items += InfoItem("RSSI", "${info.rssi} dBm")
        } catch (_: Exception) {}
    }

    // IPs
    try {
        for (intf in NetworkInterface.getNetworkInterfaces()) {
            for (addr in intf.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is Inet4Address)
                    items += InfoItem("${intf.name} IP", addr.hostAddress ?: "N/A")
            }
        }
    } catch (_: Exception) {}

    // Carrier
    try {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (tm.networkOperatorName.isNotBlank()) items += InfoItem(Strings.operator_, tm.networkOperatorName)
        if (tm.simOperatorName.isNotBlank()) items += InfoItem("SIM", tm.simOperatorName)
        if (tm.networkCountryIso.isNotBlank()) items += InfoItem(s("Страна сети", "Network Country"), tm.networkCountryIso.uppercase())
    } catch (_: Exception) {}

    // DNS
    try {
        val dns = System.getProperty("net.dns1")
        if (!dns.isNullOrBlank()) items += InfoItem("DNS", dns)
    } catch (_: Exception) {}

    return items
}

// ═══════════════════════════════════
// Sensors
// ═══════════════════════════════════
private fun getSensorInfo(context: Context): List<InfoItem> {
    val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val sensors = sm.getSensorList(Sensor.TYPE_ALL)
    if (sensors.isEmpty()) return listOf(InfoItem(Strings.noSensors, ""))
    return sensors.map { s ->
        val typeStr = when (s.type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_PRESSURE -> "Barometer"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Accel"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation"
            Sensor.TYPE_STEP_COUNTER -> "Step Counter"
            Sensor.TYPE_STEP_DETECTOR -> "Step Detector"
            else -> ""
        }
        val label = if (typeStr.isNotEmpty()) "$typeStr: ${s.name}" else s.name
        InfoItem(label, "${s.vendor} v${s.version}")
    }
}

// ═══════════════════════════════════
// Camera
// ═══════════════════════════════════
data class CameraBlock(val title: String, val facing: String, val items: List<InfoItem>)

private fun getCameraInfo(context: Context): List<InfoItem> = emptyList() // kept for compat

private fun getCameraBlocks(context: Context): List<CameraBlock> {
    val blocks = mutableListOf<CameraBlock>()
    try {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val rearIndex = intArrayOf(0) // track rear camera numbering
        val frontIndex = intArrayOf(0)

        for (id in cm.cameraIdList) {
            val items = mutableListOf<InfoItem>()
            val chars = cm.getCameraCharacteristics(id)
            val facingInt = chars.get(CameraCharacteristics.LENS_FACING)
            val isFront = facingInt == CameraCharacteristics.LENS_FACING_FRONT

            // Resolution
            val configMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val jpegSizes = configMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            val maxSize = jpegSizes?.maxByOrNull { it.width * it.height }
            val mp = if (maxSize != null) maxSize.width.toLong() * maxSize.height / 1_000_000.0 else 0.0

            // Focal length for camera type detection
            val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)

            // Determine camera type
            val cameraType: String
            val facingType: String
            if (isFront) {
                frontIndex[0]++
                // Check if it's a ToF/depth sensor (very low MP or IR)
                val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val isDepth = caps != null && caps.contains(8) // DEPTH_OUTPUT = 8
                val isToF = isDepth || (mp < 1.0 && !isFront)
                if (isToF) {
                    cameraType = s("ToF / Глубина", "ToF / Depth")
                    facingType = "tof"
                } else {
                    cameraType = if (frontIndex[0] == 1) s("Фронтальная", "Front") else s("Фронтальная", "Front") + " ${frontIndex[0]}"
                    facingType = "front"
                }
            } else {
                rearIndex[0]++
                // Detect type by focal length
                val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val isDepth = caps != null && caps.contains(8)
                if (isDepth || mp < 0.5) {
                    cameraType = s("ToF / Глубина", "ToF / Depth")
                    facingType = "tof"
                } else if (focalLengths != null && sensorSize != null && focalLengths.isNotEmpty()) {
                    val fov = 2 * Math.toDegrees(Math.atan((sensorSize.width / (2 * focalLengths[0])).toDouble()))
                    cameraType = when {
                        fov > 90 -> s("Ультраширокая", "Ultra Wide")
                        fov < 40 -> s("Телеобъектив", "Telephoto")
                        fov < 55 -> s("Портретная", "Portrait")
                        else -> s("Основная", "Main")
                    }
                    facingType = "rear"
                } else {
                    cameraType = if (rearIndex[0] == 1) s("Основная", "Main") else s("Задняя", "Rear") + " ${rearIndex[0]}"
                    facingType = "rear"
                }
            }

            // Title
            val title = "$cameraType (ID: $id)"

            // --- Fill items ---
            if (maxSize != null) {
                items += InfoItem(s("Мегапиксели", "Megapixels"), "${"%.1f".format(mp)} MP")
                items += InfoItem(s("Разрешение", "Resolution"), "${maxSize.width}×${maxSize.height}")
            }

            // All resolutions
            if (jpegSizes != null && jpegSizes.size > 1) {
                items += InfoItem(s("Разрешения", "Resolutions"),
                    jpegSizes.sortedByDescending { it.width * it.height }.take(5).joinToString(", ") { "${it.width}×${it.height}" })
            }

            // Sensor size
            if (sensorSize != null) {
                items += InfoItem(s("Сенсор", "Sensor"), "${"%.2f".format(sensorSize.width)}×${"%.2f".format(sensorSize.height)} mm")
                val diag = Math.sqrt((sensorSize.width * sensorSize.width + sensorSize.height * sensorSize.height).toDouble())
                val inchType = 43.27 / diag
                if (inchType in 0.5..15.0) items += InfoItem(s("Тип сенсора", "Sensor Type"), "1/${"%.1f".format(inchType)}\"")
            }

            // Pixel array
            val pixelArray = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            if (pixelArray != null) items += InfoItem(s("Пиксельный массив", "Pixel Array"), "${pixelArray.width}×${pixelArray.height}")

            // Focal length
            if (focalLengths != null && focalLengths.isNotEmpty())
                items += InfoItem(s("Фокусное", "Focal Length"), focalLengths.joinToString(", ") { "${"%.2f".format(it)} mm" })

            // Aperture
            val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)
            if (apertures != null && apertures.isNotEmpty())
                items += InfoItem(s("Диафрагма", "Aperture"), apertures.joinToString(", ") { "f/${"%.1f".format(it)}" })

            // FOV + 35mm equiv
            if (focalLengths != null && sensorSize != null && focalLengths.isNotEmpty()) {
                val fov = 2 * Math.toDegrees(Math.atan((sensorSize.width / (2 * focalLengths[0])).toDouble()))
                items += InfoItem(s("Угол обзора", "FOV"), "${"%.1f".format(fov)}°")
                val cropFactor = 43.27 / Math.sqrt((sensorSize.width * sensorSize.width + sensorSize.height * sensorSize.height).toDouble())
                items += InfoItem(s("Экв. 35мм", "35mm Equiv"), "${"%.0f".format(focalLengths[0] * cropFactor)} mm")
            }

            // OIS
            val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            items += InfoItem("OIS", if (oisModes != null && oisModes.any { it != 0 }) s("Да", "Yes") else s("Нет", "No"))

            // Flash
            val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            items += InfoItem(s("Вспышка", "Flash"), if (hasFlash) s("Да", "Yes") else s("Нет", "No"))

            // ISO range
            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            if (isoRange != null) items += InfoItem("ISO", "${isoRange.lower} — ${isoRange.upper}")

            // Exposure range
            val exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            if (exposureRange != null) {
                val minExp = if (exposureRange.lower > 0) "1/${1_000_000_000L / exposureRange.lower}" else "?"
                val maxExp = "${"%.1f".format(exposureRange.upper / 1_000_000_000.0)}s"
                items += InfoItem(s("Выдержка", "Exposure"), "$minExp — $maxExp")
            }

            // Max zoom
            val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)
            if (maxZoom != null && maxZoom > 1f) items += InfoItem(s("Макс. зум", "Max Zoom"), "${"%.1f".format(maxZoom)}x")

            // AF
            val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
            if (afModes != null && afModes.isNotEmpty()) {
                val names = afModes.map { when (it) { 0 -> "OFF"; 1 -> "AUTO"; 2 -> "MACRO"; 3 -> "CONT_VIDEO"; 4 -> "CONT_PIC"; 5 -> "EDOF"; else -> "$it" } }
                items += InfoItem("AF", names.joinToString(", "))
            }

            // AE/AWB
            val aeModes = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)
            if (aeModes != null) items += InfoItem("AE", "${aeModes.size} ${s("режимов", "modes")}")
            val awbModes = chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)
            if (awbModes != null) items += InfoItem("AWB", "${awbModes.size} ${s("режимов", "modes")}")

            // Video
            val videoSizes = configMap?.getOutputSizes(android.media.MediaRecorder::class.java)
            val maxVideo = videoSizes?.maxByOrNull { it.width * it.height }
            if (maxVideo != null) items += InfoItem(s("Макс. видео", "Max Video"), "${maxVideo.width}×${maxVideo.height}")

            // FPS
            val fpsRanges = chars.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            if (fpsRanges != null && fpsRanges.isNotEmpty()) {
                items += InfoItem(s("Макс. FPS", "Max FPS"), "${fpsRanges.maxOf { it.upper }}")
                items += InfoItem("FPS", fpsRanges.distinctBy { "${it.lower}-${it.upper}" }.take(5).joinToString(", ") { "${it.lower}-${it.upper}" })
            }

            // Scenes / Effects / NR
            val sceneModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES)
            if (sceneModes != null && sceneModes.isNotEmpty()) items += InfoItem(s("Сцены", "Scenes"), "${sceneModes.size}")
            val effects = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS)
            if (effects != null && effects.size > 1) items += InfoItem(s("Эффекты", "Effects"), "${effects.size}")
            val nrModes = chars.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)
            if (nrModes != null) items += InfoItem(s("Шумоподавление", "Noise Reduction"), "${nrModes.size} ${s("режимов", "modes")}")

            // Capabilities
            val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            if (capabilities != null) {
                val capNames = capabilities.map { when (it) {
                    0 -> "BACKWARD_COMPATIBLE"; 1 -> "MANUAL_SENSOR"; 2 -> "MANUAL_POST_PROC"
                    3 -> "RAW"; 4 -> "PRIVATE_REPROCESSING"; 5 -> "READ_SENSOR_SETTINGS"
                    6 -> "BURST_CAPTURE"; 7 -> "YUV_REPROCESSING"; 8 -> "DEPTH_OUTPUT"
                    9 -> "CONSTRAINED_HIGH_SPEED"; 10 -> "MOTION_TRACKING"; 11 -> "LOGICAL_MULTI_CAMERA"
                    12 -> "MONOCHROME"; 13 -> "SECURE_IMAGE_DATA"; 14 -> "SYSTEM_CAMERA"
                    15 -> "OFFLINE_PROCESSING"; else -> "$it"
                }}
                items += InfoItem(s("Возможности", "Capabilities"), capNames.joinToString(", "))
            }

            // HW Level
            val hwLevel = chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            items += InfoItem(s("Уровень", "HW Level"), when (hwLevel) { 0 -> "LIMITED"; 1 -> "FULL"; 2 -> "LEGACY"; 3 -> "LEVEL_3"; 4 -> "EXTERNAL"; else -> "$hwLevel" })

            blocks += CameraBlock(title, facingType, items)
        }
    } catch (e: Exception) {
        blocks += CameraBlock(Strings.cameraSection, "rear", listOf(InfoItem(Strings.unknown, e.message ?: "N/A")))
    }
    return blocks
}

private fun fmtBytes(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024L * 1024 -> "%.1f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / (1024.0 * 1024))
    else -> "%.2f GB".format(b / (1024.0 * 1024 * 1024))
}
