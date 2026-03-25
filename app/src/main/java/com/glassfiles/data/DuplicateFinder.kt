package com.glassfiles.data

import java.io.File
import java.security.MessageDigest

data class DuplicateGroup(
    val hash: String,
    val size: Long,
    val files: List<File>
)

object DuplicateFinder {

    fun findDuplicates(
        rootPath: String,
        onProgress: (scanned: Int, found: Int) -> Unit = { _, _ -> }
    ): List<DuplicateGroup> {
        val sizeMap = mutableMapOf<Long, MutableList<File>>()
        var scanned = 0

        // Phase 1: group by size
        fun scan(dir: File) {
            dir.listFiles()?.forEach { f ->
                if (f.isDirectory) {
                    if (!f.name.startsWith(".") && f.name != "Android") scan(f)
                } else if (f.isFile && f.length() > 1024) { // skip tiny files
                    sizeMap.getOrPut(f.length()) { mutableListOf() }.add(f)
                    scanned++
                    if (scanned % 200 == 0) onProgress(scanned, 0)
                }
            }
        }
        scan(File(rootPath))

        // Phase 2: hash files with same size
        val duplicates = mutableListOf<DuplicateGroup>()
        var found = 0
        sizeMap.filter { it.value.size > 1 }.forEach { (size, files) ->
            val hashMap = mutableMapOf<String, MutableList<File>>()
            files.forEach { f ->
                try {
                    val hash = quickHash(f)
                    hashMap.getOrPut(hash) { mutableListOf() }.add(f)
                } catch (_: Exception) {}
            }
            hashMap.filter { it.value.size > 1 }.forEach { (hash, dupes) ->
                duplicates.add(DuplicateGroup(hash, size, dupes))
                found += dupes.size
                onProgress(scanned, found)
            }
        }

        return duplicates.sortedByDescending { it.size * it.files.size }
    }

    private fun quickHash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
                total += n
                if (total >= 1024 * 1024) break // hash first 1MB only for speed
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}
