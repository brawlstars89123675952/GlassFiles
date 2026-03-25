package com.glassfiles.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileOperations {
    data class OpResult(val success: Boolean, val message: String, val destPath: String = "")

    suspend fun copy(source: File, destDir: File, onProgress: (Float) -> Unit = {}): OpResult = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) return@withContext OpResult(false, "File not found")
            destDir.mkdirs()
            if (source.isDirectory) {
                val allFiles = source.walkTopDown().filter { it.isFile }.toList()
                val total = allFiles.sumOf { it.length() }.toFloat().coerceAtLeast(1f)
                var copied = 0L
                val destFolder = File(destDir, source.name)
                allFiles.forEach { file ->
                    val destFile = File(destFolder, file.relativeTo(source).path)
                    destFile.parentFile?.mkdirs()
                    copyFile(file, destFile) { copied += it; onProgress(copied / total) }
                }
                OpResult(true, "Copied: ${source.name}", destFolder.absolutePath)
            } else {
                val destFile = File(destDir, source.name)
                val total = source.length().toFloat().coerceAtLeast(1f)
                var copied = 0L
                copyFile(source, destFile) { copied += it; onProgress(copied / total) }
                OpResult(true, "Copied: ${source.name}", destFile.absolutePath)
            }
        } catch (e: Exception) { OpResult(false, "Error: ${e.message}") }
    }

    suspend fun move(source: File, destDir: File, onProgress: (Float) -> Unit = {}): OpResult = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) return@withContext OpResult(false, "File not found")
            destDir.mkdirs()
            val dest = File(destDir, source.name)
            if (source.renameTo(dest)) { onProgress(1f); return@withContext OpResult(true, "Moved: ${source.name}", dest.absolutePath) }
            val r = copy(source, destDir, onProgress)
            if (r.success) { source.deleteRecursively(); OpResult(true, "Moved: ${source.name}", r.destPath) } else r
        } catch (e: Exception) { OpResult(false, "Error: ${e.message}") }
    }

    suspend fun delete(source: File): OpResult = withContext(Dispatchers.IO) {
        try {
            val name = source.name; val ok = source.deleteRecursively()
            OpResult(ok, if (ok) "Deleted: $name" else "Delete error")
        } catch (e: Exception) { OpResult(false, "Error: ${e.message}") }
    }

    suspend fun rename(source: File, newName: String): OpResult = withContext(Dispatchers.IO) {
        try {
            val dest = File(source.parent, newName)
            if (dest.exists()) return@withContext OpResult(false, "Already exists")
            OpResult(source.renameTo(dest), "Renamed", dest.absolutePath)
        } catch (e: Exception) { OpResult(false, "Error: ${e.message}") }
    }

    suspend fun createFolder(parent: File, name: String): OpResult = withContext(Dispatchers.IO) {
        try {
            val f = File(parent, name); if (f.exists()) return@withContext OpResult(false, "Already exists")
            OpResult(f.mkdirs(), "Created: $name", f.absolutePath)
        } catch (e: Exception) { OpResult(false, "Error: ${e.message}") }
    }

    private fun copyFile(src: File, dst: File, onBytes: (Long) -> Unit) {
        FileInputStream(src).use { fis -> FileOutputStream(dst).use { fos ->
            val buf = ByteArray(8192); var n: Int
            while (fis.read(buf).also { n = it } != -1) { fos.write(buf, 0, n); onBytes(n.toLong()) }
        } }
    }
}
