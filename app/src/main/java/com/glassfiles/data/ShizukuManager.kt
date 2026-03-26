package com.glassfiles.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * ShizukuManager — executes privileged commands through Shizuku's binder.
 * Requires Shizuku app to be installed and running on the device.
 * Uses IPC shell execution via "sh" service — no root required.
 */
object ShizukuManager {

    private const val TAG = "ShizukuManager"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    // ═══════════════════════════════════
    // Status checks
    // ═══════════════════════════════════

    fun isShizukuInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isShizukuRunning(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("pingBinder")
            method.invoke(null) as Boolean
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku not available: ${e.message}")
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("checkSelfPermission")
            (method.invoke(null) as Int) == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            Log.w(TAG, "Cannot check permission: ${e.message}")
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        try {
            val clazz = Class.forName("rikka.shizuku.Shizuku")
            val method = clazz.getMethod("requestPermission", Int::class.java)
            method.invoke(null, requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot request permission: ${e.message}")
        }
    }

    // ═══════════════════════════════════
    // Shell execution via Shizuku
    // ═══════════════════════════════════

    private suspend fun executeShell(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val shizukuClass = Class.forName("rikka.shizuku.Shizuku")
            val newProcessMethod = shizukuClass.getMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()
            ShellResult(exitCode == 0, stdout.trim(), stderr.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Shell exec failed: ${e.message}")
            ShellResult(false, "", e.message ?: "Unknown error")
        }
    }

    // ═══════════════════════════════════
    // App management
    // ═══════════════════════════════════

    /** Force stop an application */
    suspend fun forceStop(packageName: String): Boolean {
        val result = executeShell("am force-stop $packageName")
        return result.success
    }

    /** Freeze (disable) an application */
    suspend fun freezeApp(packageName: String): Boolean {
        val result = executeShell("pm disable-user --user 0 $packageName")
        return result.success
    }

    /** Unfreeze (enable) an application */
    suspend fun unfreezeApp(packageName: String): Boolean {
        val result = executeShell("pm enable --user 0 $packageName")
        return result.success
    }

    /** Clear cache of an application */
    suspend fun clearCache(packageName: String): Boolean {
        val result = executeShell("pm clear --cache-only $packageName")
        // Fallback for older Android versions
        if (!result.success) {
            val fallback = executeShell("rm -rf /data/data/$packageName/cache/*")
            return fallback.success
        }
        return result.success
    }

    /** Install APK silently (without confirmation dialog) */
    suspend fun silentInstall(apkPath: String): Boolean {
        val result = executeShell("pm install -r -t \"$apkPath\"")
        return result.success
    }

    /** Check if app is frozen/disabled */
    suspend fun isAppFrozen(packageName: String): Boolean {
        val result = executeShell("pm list packages -d")
        return result.stdout.contains(packageName)
    }

    // ═══════════════════════════════════
    // File access (Android/data, Android/obb)
    // ═══════════════════════════════════

    /** List files in Android/data or Android/obb via Shizuku shell */
    suspend fun listRestrictedDir(path: String): List<ShizukuFileItem> = withContext(Dispatchers.IO) {
        val result = executeShell("ls -la \"$path\"")
        if (!result.success) return@withContext emptyList()

        result.stdout.lines().mapNotNull { line ->
            parseListLine(line, path)
        }
    }

    /** Copy file from restricted directory to accessible location */
    suspend fun copyFromRestricted(sourcePath: String, destPath: String): Boolean {
        val result = executeShell("cp -r \"$sourcePath\" \"$destPath\"")
        return result.success
    }

    /** Copy file to restricted directory */
    suspend fun copyToRestricted(sourcePath: String, destPath: String): Boolean {
        val result = executeShell("cp -r \"$sourcePath\" \"$destPath\"")
        return result.success
    }

    /** Delete file/folder from restricted directory */
    suspend fun deleteFromRestricted(path: String): Boolean {
        val result = executeShell("rm -rf \"$path\"")
        return result.success
    }

    /** Rename file in restricted directory */
    suspend fun renameInRestricted(oldPath: String, newPath: String): Boolean {
        val result = executeShell("mv \"$oldPath\" \"$newPath\"")
        return result.success
    }

    /** Get size of a directory via Shizuku */
    suspend fun getDirectorySize(path: String): Long {
        val result = executeShell("du -sb \"$path\" 2>/dev/null | cut -f1")
        return result.stdout.trim().toLongOrNull() ?: 0L
    }

    /** Change file permissions */
    suspend fun chmod(path: String, mode: String): Boolean {
        val result = executeShell("chmod $mode \"$path\"")
        return result.success
    }

    /** Change file owner */
    suspend fun chown(path: String, owner: String): Boolean {
        val result = executeShell("chown $owner \"$path\"")
        return result.success
    }

    /** Create symbolic link */
    suspend fun symlink(target: String, linkPath: String): Boolean {
        val result = executeShell("ln -s \"$target\" \"$linkPath\"")
        return result.success
    }

    /** List system directory (e.g. /data/local/tmp) */
    suspend fun listSystemDir(path: String): List<ShizukuFileItem> = listRestrictedDir(path)

    // ═══════════════════════════════════
    // Advanced app management
    // ═══════════════════════════════════

    /** Uninstall app without confirmation */
    suspend fun uninstallApp(packageName: String): Boolean {
        val result = executeShell("pm uninstall $packageName")
        return result.success
    }

    /** Downgrade install APK */
    suspend fun downgradeInstall(apkPath: String): Boolean {
        val result = executeShell("pm install -r -d -t \"$apkPath\"")
        return result.success
    }

    /** Clear ALL app data (not just cache) */
    suspend fun clearAllData(packageName: String): Boolean {
        val result = executeShell("pm clear $packageName")
        return result.success
    }

    /** Get real data size of app */
    suspend fun getAppDataSize(packageName: String): Long {
        val result = executeShell("du -sb /data/data/$packageName 2>/dev/null | cut -f1")
        return result.stdout.trim().toLongOrNull() ?: 0L
    }

    /** Revoke a permission from app */
    suspend fun revokePermission(packageName: String, permission: String): Boolean {
        val result = executeShell("pm revoke $packageName $permission")
        return result.success
    }

    /** Grant a permission to app */
    suspend fun grantPermission(packageName: String, permission: String): Boolean {
        val result = executeShell("pm grant $packageName $permission")
        return result.success
    }

    /** Restrict app background activity */
    suspend fun restrictBackground(packageName: String, restrict: Boolean): Boolean {
        val op = if (restrict) "ignore" else "allow"
        val result = executeShell("appops set $packageName RUN_IN_BACKGROUND $op")
        return result.success
    }

    /** Get list of running processes with memory usage */
    suspend fun getRunningProcesses(): List<ProcessInfo> = withContext(Dispatchers.IO) {
        val result = executeShell("ps -A -o PID,RSS,NAME 2>/dev/null || ps -A")
        if (!result.success) return@withContext emptyList()
        result.stdout.lines().drop(1).mapNotNull { line ->
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size >= 3) {
                ProcessInfo(
                    pid = parts[0].toIntOrNull() ?: 0,
                    memKb = parts[1].toLongOrNull() ?: 0L,
                    name = parts.drop(2).joinToString(" ")
                )
            } else null
        }.sortedByDescending { it.memKb }
    }

    /** Get memory info for specific package */
    suspend fun getAppMemoryInfo(packageName: String): String {
        val result = executeShell("dumpsys meminfo $packageName 2>/dev/null | head -20")
        return if (result.success) result.stdout else ""
    }

    // ═══════════════════════════════════
    // System tools
    // ═══════════════════════════════════

    /** Take screenshot and save to path */
    suspend fun takeScreenshot(outputPath: String): Boolean {
        val result = executeShell("screencap -p \"$outputPath\"")
        return result.success
    }

    /** Start screen recording */
    suspend fun startScreenRecord(outputPath: String, durationSec: Int = 30): Boolean {
        val result = executeShell("screenrecord --time-limit $durationSec \"$outputPath\" &")
        return result.success
    }

    /** Stop screen recording */
    suspend fun stopScreenRecord(): Boolean {
        val result = executeShell("pkill -INT screenrecord")
        return result.success
    }

    /** Get current screen DPI */
    suspend fun getScreenDpi(): Int {
        val result = executeShell("wm density")
        val match = Regex("(\\d+)").find(result.stdout)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /** Set screen DPI */
    suspend fun setScreenDpi(dpi: Int): Boolean {
        val result = executeShell("wm density $dpi")
        return result.success
    }

    /** Reset screen DPI to default */
    suspend fun resetScreenDpi(): Boolean {
        val result = executeShell("wm density reset")
        return result.success
    }

    /** Get current screen resolution */
    suspend fun getScreenResolution(): String {
        val result = executeShell("wm size")
        return result.stdout.trim()
    }

    /** Set screen resolution */
    suspend fun setScreenResolution(width: Int, height: Int): Boolean {
        val result = executeShell("wm size ${width}x${height}")
        return result.success
    }

    /** Reset screen resolution to default */
    suspend fun resetScreenResolution(): Boolean {
        val result = executeShell("wm size reset")
        return result.success
    }

    /** Toggle WiFi */
    suspend fun setWifi(enabled: Boolean): Boolean {
        val result = executeShell("svc wifi ${if (enabled) "enable" else "disable"}")
        return result.success
    }

    /** Toggle Bluetooth */
    suspend fun setBluetooth(enabled: Boolean): Boolean {
        val result = executeShell("svc bluetooth ${if (enabled) "enable" else "disable"}")
        return result.success
    }

    /** Reboot device */
    suspend fun reboot(mode: String = ""): Boolean {
        val cmd = if (mode.isBlank()) "reboot" else "reboot $mode"
        val result = executeShell(cmd)
        return result.success
    }

    /** Get logcat (last N lines) */
    suspend fun getLogcat(lines: Int = 200, filter: String = ""): String {
        val filterArg = if (filter.isNotBlank()) " | grep -i \"$filter\"" else ""
        val result = executeShell("logcat -d -t $lines$filterArg")
        return if (result.success) result.stdout else result.stderr
    }

    /** Clear logcat */
    suspend fun clearLogcat(): Boolean {
        val result = executeShell("logcat -c")
        return result.success
    }

    /** Get battery stats summary */
    suspend fun getBatteryStats(): String {
        val result = executeShell("dumpsys battery")
        return if (result.success) result.stdout else ""
    }

    /** Get detailed battery stats per app */
    suspend fun getBatteryStatsDetailed(): String {
        val result = executeShell("dumpsys batterystats --charged 2>/dev/null | head -100")
        return if (result.success) result.stdout else ""
    }

    /** Mount/remount partition */
    suspend fun remount(partition: String, mode: String = "rw"): Boolean {
        val result = executeShell("mount -o remount,$mode $partition")
        return result.success
    }

    /** Get mount points */
    suspend fun getMounts(): String {
        val result = executeShell("mount")
        return if (result.success) result.stdout else ""
    }

    /** Backup app data to tar file */
    suspend fun backupAppData(packageName: String, outputPath: String): Boolean {
        val result = executeShell("tar -czf \"$outputPath\" -C /data/data/$packageName . 2>/dev/null")
        return result.success
    }

    // ═══════════════════════════════════
    // Data models
    // ═══════════════════════════════════

    data class ProcessInfo(val pid: Int, val memKb: Long, val name: String)

    // ═══════════════════════════════════
    // Helpers
    // ═══════════════════════════════════

    private fun parseListLine(line: String, parentPath: String): ShizukuFileItem? {
        // Parse output of `ls -la`
        // drwxrwx--x  3 system ext_data_rw 4096 2024-01-15 12:30 com.example.app
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size < 8) return null
        val perms = parts[0]
        if (perms == "total") return null
        val isDir = perms.startsWith("d")
        val size = parts[4].toLongOrNull() ?: 0L
        val name = parts.drop(7).joinToString(" ") // handle filenames with spaces
        if (name == "." || name == "..") return null
        return ShizukuFileItem(
            name = name,
            path = "$parentPath/$name",
            isDirectory = isDir,
            size = size
        )
    }

    data class ShellResult(val success: Boolean, val stdout: String, val stderr: String)

    data class ShizukuFileItem(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long
    )
}
