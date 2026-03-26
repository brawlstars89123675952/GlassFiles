package com.glassfiles.data

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method

object ShizukuManager {

    private const val TAG = "ShizukuMgr"
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    // ═══════════════════════════════════
    // Status
    // ═══════════════════════════════════

    fun isShizukuInstalled(context: Context): Boolean {
        return try { context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0); true }
        catch (_: PackageManager.NameNotFoundException) { false }
    }

    fun isShizukuRunning(): Boolean {
        return try { rikka.shizuku.Shizuku.pingBinder() }
        catch (e: Exception) { Log.w(TAG, "ping: ${e.message}"); false }
    }

    fun hasShizukuPermission(): Boolean {
        return try { rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
        catch (e: Exception) { Log.w(TAG, "perm: ${e.message}"); false }
    }

    fun requestPermission(requestCode: Int) {
        try { rikka.shizuku.Shizuku.requestPermission(requestCode) }
        catch (e: Exception) { Log.e(TAG, "req: ${e.message}") }
    }

    // ═══════════════════════════════════
    // Shell execution — reflection with fallbacks
    // ═══════════════════════════════════

    private var cachedMethod: Method? = null

    private fun getNewProcessMethod(): Method? {
        if (cachedMethod != null) return cachedMethod
        return try {
            val m = rikka.shizuku.Shizuku::class.java.getDeclaredMethod(
                "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java
            )
            m.isAccessible = true
            cachedMethod = m
            m
        } catch (e: Exception) {
            Log.e(TAG, "Method lookup failed: ${e.message}")
            // Fallback: try all declared methods
            try {
                val m = rikka.shizuku.Shizuku::class.java.declaredMethods.firstOrNull { it.name == "newProcess" && it.parameterCount == 3 }
                m?.isAccessible = true
                cachedMethod = m
                m
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback lookup failed: ${e2.message}")
                null
            }
        }
    }

    private suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        try {
            val method = getNewProcessMethod()
            if (method == null) return@withContext ShellResult(false, "", "Shizuku newProcess method not found")

            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()
            process.destroy()
            Log.d(TAG, "exec[$command] exit=$exitCode stdout=${stdout.take(100)}")
            ShellResult(exitCode == 0, stdout.trim(), stderr.trim())
        } catch (e: java.lang.reflect.InvocationTargetException) {
            val cause = e.cause ?: e
            Log.e(TAG, "exec invoke error[$command]: ${cause.message}")
            ShellResult(false, "", "Invoke: ${cause.message}")
        } catch (e: Exception) {
            Log.e(TAG, "exec error[$command]: ${e.message}")
            ShellResult(false, "", "Error: ${e.message}")
        }
    }

    // ═══════════════════════════════════
    // App management
    // ═══════════════════════════════════

    suspend fun forceStop(pkg: String): Boolean = exec("am force-stop $pkg").success
    suspend fun freezeApp(pkg: String): Boolean = exec("pm disable-user --user 0 $pkg").success
    suspend fun unfreezeApp(pkg: String): Boolean = exec("pm enable --user 0 $pkg").success

    suspend fun clearCache(pkg: String): Boolean {
        val r = exec("pm clear --cache-only $pkg")
        return if (r.success) true else exec("rm -rf /data/data/$pkg/cache/*").success
    }

    suspend fun silentInstall(apkPath: String): Boolean = exec("pm install -r -t \"$apkPath\"").success
    suspend fun uninstallApp(pkg: String): Boolean = exec("pm uninstall $pkg").success
    suspend fun downgradeInstall(apkPath: String): Boolean = exec("pm install -r -d -t \"$apkPath\"").success
    suspend fun clearAllData(pkg: String): Boolean = exec("pm clear $pkg").success

    suspend fun isAppFrozen(pkg: String): Boolean {
        val r = exec("pm list packages -d")
        return r.stdout.contains(pkg)
    }

    suspend fun getAppDataSize(pkg: String): Long {
        val r = exec("du -sb /data/data/$pkg 2>/dev/null | cut -f1")
        return r.stdout.trim().toLongOrNull() ?: 0L
    }

    suspend fun revokePermission(pkg: String, perm: String): Boolean = exec("pm revoke $pkg $perm").success
    suspend fun grantPermission(pkg: String, perm: String): Boolean = exec("pm grant $pkg $perm").success

    suspend fun restrictBackground(pkg: String, restrict: Boolean): Boolean {
        val op = if (restrict) "ignore" else "allow"
        return exec("appops set $pkg RUN_IN_BACKGROUND $op").success
    }

    suspend fun backupAppData(pkg: String, outputPath: String): Boolean =
        exec("tar -czf \"$outputPath\" -C /data/data/$pkg . 2>/dev/null").success

    // ═══════════════════════════════════
    // File operations
    // ═══════════════════════════════════

    /** List directory with improved parsing and error feedback */
    suspend fun listRestrictedDir(path: String): List<ShizukuFileItem> = withContext(Dispatchers.IO) {
        // Try ls -la first
        var r = exec("ls -la \"$path\" 2>&1")
        if (!r.success && r.stdout.isBlank()) {
            // Fallback: simple ls
            r = exec("ls -1 \"$path\" 2>&1")
            if (!r.success) {
                Log.e(TAG, "listDir failed: ${r.stderr}")
                return@withContext emptyList()
            }
            // Simple parsing: one name per line
            return@withContext r.stdout.lines().filter { it.isNotBlank() && it != "." && it != ".." }.map { name ->
                val isDir = exec("test -d \"$path/$name\" && echo D || echo F").stdout.trim() == "D"
                ShizukuFileItem(name.trim(), "$path/${name.trim()}", isDir, 0L)
            }
        }

        val items = r.stdout.lines().mapNotNull { line -> parseListLine(line, path) }
        if (items.isEmpty() && r.stdout.isNotBlank()) {
            Log.w(TAG, "Parse returned 0 from: ${r.stdout.take(200)}")
        }
        items
    }

    /** Get last listing error for UI display */
    suspend fun getLastError(path: String): String {
        val r = exec("ls -la \"$path\" 2>&1")
        return if (r.success) "" else r.stderr.ifBlank { r.stdout }
    }

    suspend fun copyFromRestricted(src: String, dest: String): Boolean = exec("cp -r \"$src\" \"$dest\"").success
    suspend fun copyToRestricted(src: String, dest: String): Boolean = exec("cp -r \"$src\" \"$dest\"").success
    suspend fun deleteFromRestricted(path: String): Boolean = exec("rm -rf \"$path\"").success
    suspend fun renameInRestricted(old: String, new: String): Boolean = exec("mv \"$old\" \"$new\"").success
    suspend fun getDirectorySize(path: String): Long {
        val r = exec("du -sb \"$path\" 2>/dev/null | cut -f1")
        return r.stdout.trim().toLongOrNull() ?: 0L
    }

    suspend fun chmod(path: String, mode: String): Boolean = exec("chmod $mode \"$path\"").success
    suspend fun chown(path: String, owner: String): Boolean = exec("chown $owner \"$path\"").success
    suspend fun symlink(target: String, linkPath: String): Boolean = exec("ln -s \"$target\" \"$linkPath\"").success

    // ═══════════════════════════════════
    // System
    // ═══════════════════════════════════

    suspend fun takeScreenshot(out: String): Boolean = exec("screencap -p \"$out\"").success
    suspend fun startScreenRecord(out: String, sec: Int = 30): Boolean = exec("screenrecord --time-limit $sec \"$out\" &").success
    suspend fun stopScreenRecord(): Boolean = exec("pkill -INT screenrecord").success

    suspend fun getScreenDpi(): Int {
        val r = exec("wm density")
        return Regex("(\\d+)").findAll(r.stdout).lastOrNull()?.value?.toIntOrNull() ?: 0
    }
    suspend fun setScreenDpi(dpi: Int): Boolean = exec("wm density $dpi").success
    suspend fun resetScreenDpi(): Boolean = exec("wm density reset").success
    suspend fun getScreenResolution(): String = exec("wm size").let { if (it.success) it.stdout.trim() else "" }
    suspend fun setScreenResolution(w: Int, h: Int): Boolean = exec("wm size ${w}x${h}").success
    suspend fun resetScreenResolution(): Boolean = exec("wm size reset").success

    suspend fun setWifi(on: Boolean): Boolean = exec("svc wifi ${if (on) "enable" else "disable"}").success
    suspend fun setBluetooth(on: Boolean): Boolean = exec("svc bluetooth ${if (on) "enable" else "disable"}").success
    suspend fun reboot(mode: String = ""): Boolean = exec(if (mode.isBlank()) "reboot" else "reboot $mode").success

    suspend fun getLogcat(lines: Int = 200, filter: String = ""): String {
        val grep = if (filter.isNotBlank()) " | grep -i \"$filter\"" else ""
        val r = exec("logcat -d -t $lines$grep")
        return if (r.success) r.stdout else r.stderr
    }
    suspend fun clearLogcat(): Boolean = exec("logcat -c").success
    suspend fun getBatteryStats(): String = exec("dumpsys battery").let { if (it.success) it.stdout else "" }

    suspend fun getRunningProcesses(): List<ProcessInfo> = withContext(Dispatchers.IO) {
        val r = exec("ps -A -o PID,RSS,NAME 2>/dev/null || ps -A")
        if (!r.success) return@withContext emptyList()
        r.stdout.lines().drop(1).mapNotNull { line ->
            val p = line.trim().split("\\s+".toRegex())
            if (p.size >= 3) ProcessInfo(p[0].toIntOrNull() ?: 0, p[1].toLongOrNull() ?: 0L, p.drop(2).joinToString(" "))
            else null
        }.sortedByDescending { it.memKb }
    }

    suspend fun getMounts(): String = exec("mount").let { if (it.success) it.stdout else "" }

    // ═══════════════════════════════════
    // Helpers
    // ═══════════════════════════════════

    private fun parseListLine(line: String, parentPath: String): ShizukuFileItem? {
        val trimmed = line.trim()
        if (trimmed.isBlank() || trimmed.startsWith("total")) return null
        val parts = trimmed.split("\\s+".toRegex())
        if (parts.size < 7) return null

        val perms = parts[0]
        val isDir = perms.startsWith("d") || perms.startsWith("l")
        val size = parts.getOrNull(4)?.toLongOrNull() ?: 0L

        // Name can start at index 7 or 8 depending on ls format
        // Try to find the name by looking for date patterns
        val nameStartIdx = findNameIndex(parts)
        if (nameStartIdx < 0 || nameStartIdx >= parts.size) return null

        val name = parts.drop(nameStartIdx).joinToString(" ")
            .let { if (it.contains(" -> ")) it.substringBefore(" -> ") else it } // handle symlinks
        if (name.isBlank() || name == "." || name == "..") return null

        return ShizukuFileItem(name, "$parentPath/$name", isDir, size)
    }

    /** Find where filename starts in ls -la output by detecting date/time columns */
    private fun findNameIndex(parts: List<String>): Int {
        // Common ls -la formats:
        // drwxrwx--x 3 system ext_data_rw 4096 2024-01-15 12:30 dirname
        // drwxrwx--x 3 system ext_data_rw 4096 Jan 15 12:30 dirname
        for (i in 5 until parts.size) {
            // Check for time pattern HH:MM or HH:MM:SS
            if (parts[i].matches(Regex("\\d{1,2}:\\d{2}(:\\d{2})?"))) {
                return i + 1
            }
            // Check for year pattern (4 digits alone)
            if (parts[i].matches(Regex("\\d{4}")) && i > 5) {
                return i + 1
            }
        }
        // Fallback: assume index 7 (standard format)
        return if (parts.size > 7) 7 else -1
    }

    data class ShellResult(val success: Boolean, val stdout: String, val stderr: String)
    data class ShizukuFileItem(val name: String, val path: String, val isDirectory: Boolean, val size: Long)
    data class ProcessInfo(val pid: Int, val memKb: Long, val name: String)
}

