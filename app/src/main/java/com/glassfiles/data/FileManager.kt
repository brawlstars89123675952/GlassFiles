package com.glassfiles.data

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Реальная работа с файловой системой Android.
 * Использует Environment + java.io.File для внутреннего хранилища,
 * и MediaStore для недавних файлов.
 */
object FileManager {

    /**
     * Получить список файлов/папок в директории
     */
    fun listFiles(path: String, showHidden: Boolean = false): List<FileItem> {
        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        return dir.listFiles()
            ?.filter { showHidden || !it.name.startsWith(".") }
            ?.sortedWith(compareByDescending<File> { it.isDirectory }.thenBy { it.name.lowercase() })
            ?.map { file ->
                val ext = file.extension
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    size = if (file.isFile) file.length() else 0,
                    lastModified = file.lastModified(),
                    type = if (file.isDirectory) FileType.FOLDER else getFileType(ext),
                    isDirectory = file.isDirectory,
                    folderColor = pickFolderColor(file.name),
                    itemCount = if (file.isDirectory) file.listFiles()?.size ?: 0 else 0,
                    extension = ext,
                    isDownloaded = true
                )
            } ?: emptyList()
    }

    /**
     * Корневые локации устройства
     */
    fun getStorageLocations(): List<StorageLocation> {
        val locations = mutableListOf<StorageLocation>()

        // Внутреннее хранилище
        val internal = Environment.getExternalStorageDirectory()
        if (internal.exists()) {
            locations.add(
                StorageLocation(
                    name = Strings.onMyDevice,
                    icon = "phone_android",
                    path = internal.absolutePath,
                    color = androidx.compose.ui.graphics.Color(0xFF007AFF)
                )
            )
        }

        // Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads.exists()) {
            locations.add(
                StorageLocation(
                    name = Strings.downloads,
                    icon = "download",
                    path = downloads.absolutePath,
                    color = androidx.compose.ui.graphics.Color(0xFF007AFF)
                )
            )
        }

        // Documents
        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents.exists()) {
            locations.add(
                StorageLocation(
                    name = Strings.documents,
                    icon = "folder",
                    path = documents.absolutePath,
                    color = androidx.compose.ui.graphics.Color(0xFF007AFF)
                )
            )
        }

        // DCIM
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (dcim.exists()) {
            locations.add(
                StorageLocation(
                    name = Strings.photos,
                    icon = "photo",
                    path = dcim.absolutePath,
                    color = androidx.compose.ui.graphics.Color(0xFFFF9500)
                )
            )
        }

        // Music
        val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (music.exists()) {
            locations.add(
                StorageLocation(
                    name = Strings.music,
                    icon = "music",
                    path = music.absolutePath,
                    color = androidx.compose.ui.graphics.Color(0xFFFF2D55)
                )
            )
        }

        return locations
    }

    /**
     * Получить избранные папки (Downloads + Documents)
     */
    fun getFavorites(): List<FileItem> {
        val favorites = mutableListOf<FileItem>()

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (downloads.exists()) {
            favorites.add(
                FileItem(
                    name = Strings.downloads,
                    path = downloads.absolutePath,
                    isDirectory = true,
                    type = FileType.FOLDER,
                    folderColor = FolderColor.BLUE,
                    itemCount = downloads.listFiles()?.size ?: 0
                )
            )
        }

        val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (documents.exists()) {
            favorites.add(
                FileItem(
                    name = Strings.documents,
                    path = documents.absolutePath,
                    isDirectory = true,
                    type = FileType.FOLDER,
                    folderColor = FolderColor.BLUE,
                    itemCount = documents.listFiles()?.size ?: 0
                )
            )
        }

        return favorites
    }

    /**
     * Недавние файлы через MediaStore
     */
    fun getRecentFiles(context: Context, limit: Int = 30): List<FileItem> {
        val files = mutableListOf<FileItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
        val selection = "${MediaStore.Files.FileColumns.SIZE} > 0"

        try {
            context.contentResolver.query(
                MediaStore.Files.getContentUri("external"),
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val name = cursor.getString(nameCol) ?: continue
                    val path = cursor.getString(pathCol) ?: continue
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol) * 1000 // seconds → millis

                    // Skip hidden files
                    if (name.startsWith(".")) continue

                    val ext = name.substringAfterLast('.', "")
                    files.add(
                        FileItem(
                            name = name,
                            path = path,
                            size = size,
                            lastModified = date,
                            type = getFileType(ext),
                            isDirectory = false,
                            extension = ext,
                            isDownloaded = true
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return files
    }

    /**
     * Подбор цвета папки по имени (как у iOS — разные цвета для спецпапок)
     */
    private fun pickFolderColor(name: String): FolderColor {
        return when {
            name.equals("Download", true) || name.equals("Downloads", true) -> FolderColor.BLUE
            name.equals("DCIM", true) || name.contains("Photo", true) || name.contains("Camera", true) -> FolderColor.YELLOW
            name.equals("Music", true) || name.contains("Audio", true) -> FolderColor.RED
            name.equals("Documents", true) -> FolderColor.BLUE
            name.equals("Movies", true) || name.contains("Video", true) -> FolderColor.PURPLE
            name.contains("Android", true) -> FolderColor.GREEN
            name.contains("Telegram", true) -> FolderColor.BLUE
            name.contains("WhatsApp", true) -> FolderColor.GREEN
            else -> FolderColor.BLUE
        }
    }
}
