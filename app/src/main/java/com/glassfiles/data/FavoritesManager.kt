package com.glassfiles.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FavoritesManager(context: Context) {
    private val file = File(context.filesDir, "favorites.json")
    data class Favorite(val path: String, val name: String, val isDirectory: Boolean, val addedAt: Long)

    fun getAll(): List<Favorite> {
        val arr = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
        return (0 until arr.length()).map { i -> val o = arr.getJSONObject(i)
            Favorite(o.getString("path"), o.getString("name"), o.optBoolean("dir", false), o.optLong("time", 0))
        }.filter { File(it.path).exists() }
    }

    fun isFavorite(path: String): Boolean = getAll().any { it.path == path }
    fun toggle(path: String, name: String, isDir: Boolean) { if (isFavorite(path)) remove(path) else add(path, name, isDir) }

    fun add(path: String, name: String, isDir: Boolean) {
        if (isFavorite(path)) return
        val arr = try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
        arr.put(JSONObject().apply { put("path", path); put("name", name); put("dir", isDir); put("time", System.currentTimeMillis()) })
        file.writeText(arr.toString())
    }

    fun remove(path: String) {
        val arr = try { JSONArray(file.readText()) } catch (_: Exception) { return }
        val n = JSONArray(); for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); if (o.getString("path") != path) n.put(o) }
        file.writeText(n.toString())
    }
}
