package com.glassfiles.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TrashManager(context: Context) {
    val trashDir = File(context.filesDir, ".trash").apply { mkdirs() }
    private val metaFile = File(trashDir, "metadata.json")

    data class TrashItem(val name: String, val originalPath: String, val trashedAt: Long, val size: Long, val isDirectory: Boolean) {
        val trashPath: String get() = "${trashedAt}_$name"
    }

    fun getTrashItems(): List<TrashItem> {
        val meta = loadMeta()
        return (0 until meta.length()).map { i ->
            val o = meta.getJSONObject(i)
            TrashItem(o.getString("name"), o.getString("original"), o.getLong("time"), o.optLong("size", 0), o.optBoolean("dir", false))
        }.sortedByDescending { it.trashedAt }
    }

    suspend fun moveToTrash(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val item = TrashItem(file.name, file.absolutePath, System.currentTimeMillis(), file.length(), file.isDirectory)
            val dest = File(trashDir, item.trashPath)
            if (file.renameTo(dest) || run { file.copyRecursively(dest, true); file.deleteRecursively(); true }) {
                val meta = loadMeta()
                meta.put(JSONObject().apply { put("name", item.name); put("original", item.originalPath); put("time", item.trashedAt); put("size", item.size); put("dir", item.isDirectory) })
                saveMeta(meta); true
            } else false
        } catch (_: Exception) { false }
    }

    suspend fun restore(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val src = File(trashDir, item.trashPath); val dest = File(item.originalPath); dest.parentFile?.mkdirs()
            if (src.renameTo(dest) || run { src.copyRecursively(dest, true); src.deleteRecursively(); true }) { removeMeta(item); true } else false
        } catch (_: Exception) { false }
    }

    suspend fun deletePermanently(item: TrashItem): Boolean = withContext(Dispatchers.IO) {
        try { File(trashDir, item.trashPath).deleteRecursively(); removeMeta(item); true } catch (_: Exception) { false }
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        trashDir.listFiles()?.forEach { if (it.name != "metadata.json") it.deleteRecursively() }; saveMeta(JSONArray())
    }

    fun getTrashSize(): Long = trashDir.walkTopDown().filter { it.isFile && it.name != "metadata.json" }.sumOf { it.length() }

    /** Auto-clean items older than N days */
    suspend fun autoClean(daysToKeep: Int = 30): Int = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - daysToKeep * 24L * 60 * 60 * 1000
        val items = getTrashItems().filter { it.trashedAt < cutoff }
        items.forEach { deletePermanently(it) }
        items.size
    }

    private fun loadMeta(): JSONArray = try { JSONArray(metaFile.readText()) } catch (_: Exception) { JSONArray() }
    private fun saveMeta(arr: JSONArray) { metaFile.writeText(arr.toString(2)) }
    private fun removeMeta(item: TrashItem) {
        val meta = loadMeta(); val n = JSONArray()
        for (i in 0 until meta.length()) { val o = meta.getJSONObject(i); if (o.getLong("time") != item.trashedAt || o.getString("name") != item.name) n.put(o) }
        saveMeta(n)
    }
}
