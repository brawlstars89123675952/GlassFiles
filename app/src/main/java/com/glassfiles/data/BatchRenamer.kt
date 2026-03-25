package com.glassfiles.data

import java.io.File

object BatchRenamer {

    enum class RenameMode {
        REPLACE, PREFIX, SUFFIX, SEQUENCE, LOWERCASE, UPPERCASE;
        val label: String get() = when (this) {
            REPLACE -> Strings.findAndReplace; PREFIX -> Strings.addPrefix
            SUFFIX -> Strings.addSuffix; SEQUENCE -> Strings.sequence
            LOWERCASE -> Strings.toLowercase; UPPERCASE -> Strings.toUppercase
        }
    }

    data class RenameResult(val renamed: Int, val errors: Int)

    fun preview(files: List<File>, mode: RenameMode, param1: String = "", param2: String = ""): List<Pair<String, String>> {
        return files.mapIndexed { i, file ->
            val oldName = file.nameWithoutExtension
            val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
            val newName = when (mode) {
                RenameMode.REPLACE -> oldName.replace(param1, param2) + ext
                RenameMode.PREFIX -> param1 + oldName + ext
                RenameMode.SUFFIX -> oldName + param1 + ext
                RenameMode.SEQUENCE -> "${param1}${(i + 1).toString().padStart(3, '0')}$ext"
                RenameMode.LOWERCASE -> oldName.lowercase() + ext.lowercase()
                RenameMode.UPPERCASE -> oldName.uppercase() + ext
            }
            file.name to newName
        }
    }

    fun execute(files: List<File>, mode: RenameMode, param1: String = "", param2: String = ""): RenameResult {
        var renamed = 0; var errors = 0
        val previews = preview(files, mode, param1, param2)
        files.forEachIndexed { i, file ->
            val newName = previews[i].second
            if (newName != file.name) {
                val dest = File(file.parent, newName)
                if (!dest.exists() && file.renameTo(dest)) renamed++ else errors++
            }
        }
        return RenameResult(renamed, errors)
    }
}
