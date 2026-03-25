package com.glassfiles.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color


object FileTags {
    val builtIn get() = listOf(
        FileTag(Strings.tagRed, Color(0xFFFF3B30)),
        FileTag(Strings.tagOrange, Color(0xFFFF9500)),
        FileTag(Strings.tagYellow, Color(0xFFFFCC00)),
        FileTag(Strings.tagGreen, Color(0xFF34C759)),
        FileTag(Strings.tagBlue, Color(0xFF007AFF)),
        FileTag(Strings.tagPurple, Color(0xFFAF52DE)),
        FileTag(Strings.tagGray, Color(0xFF8E8E93)),
    )

    fun byName(name: String): FileTag? = builtIn.find { it.name == name }
}

class TagManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("file_tags", Context.MODE_PRIVATE)

    /** Get tags for a file path */
    fun getTags(path: String): List<FileTag> {
        val raw = prefs.getString(key(path), null) ?: return emptyList()
        return raw.split(",").mapNotNull { FileTags.byName(it.trim()) }
    }

    /** Set tags for a file path */
    fun setTags(path: String, tags: List<FileTag>) {
        if (tags.isEmpty()) prefs.edit().remove(key(path)).apply()
        else prefs.edit().putString(key(path), tags.joinToString(",") { it.name }).apply()
    }

    /** Toggle a tag on a file */
    fun toggleTag(path: String, tag: FileTag) {
        val current = getTags(path).toMutableList()
        if (current.any { it.name == tag.name }) current.removeAll { it.name == tag.name }
        else current.add(tag)
        setTags(path, current)
    }

    /** Check if file has a specific tag */
    fun hasTag(path: String, tagName: String): Boolean {
        return getTags(path).any { it.name == tagName }
    }

    /** Get all files with a specific tag */
    fun getFilesByTag(tagName: String): List<String> {
        return prefs.all.filter { (_, v) ->
            (v as? String)?.split(",")?.any { it.trim() == tagName } == true
        }.map { it.key.removePrefix("tag_") }
    }

    private fun key(path: String) = "tag_$path"
}
