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

    /** Get size of a directory via Shizuku */
    suspend fun getDirectorySize(path: String): Long {
        val result = executeShell("du -sb \"$path\" 2>/dev/null | cut -f1")
        return result.stdout.trim().toLongOrNull() ?: 0L
    }

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
