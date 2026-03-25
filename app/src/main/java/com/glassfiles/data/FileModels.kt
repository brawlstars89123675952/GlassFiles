package com.glassfiles.data

import androidx.compose.ui.graphics.Color
import com.glassfiles.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileType {
    FOLDER, IMAGE, VIDEO, AUDIO, PDF, DOCUMENT, SPREADSHEET, PRESENTATION, ARCHIVE, CODE, TEXT, UNKNOWN
}

enum class FolderColor(val color: Color) {
    BLUE(FolderBlue), GREEN(FolderGreen), ORANGE(FolderOrange),
    RED(FolderRed), PURPLE(FolderPurple), YELLOW(FolderYellow)
}

data class FileTag(val name: String, val color: Color)

data class FileItem(
    val name: String,
    val path: String,
    val size: Long = 0,
    val lastModified: Long = System.currentTimeMillis(),
    val type: FileType = FileType.UNKNOWN,
    val isDirectory: Boolean = false,
    val folderColor: FolderColor = FolderColor.BLUE,
    val itemCount: Int = 0,
    val tags: List<FileTag> = emptyList(),
    val extension: String = "",
    val isDownloaded: Boolean = true,
) {
    val formattedSize: String get() = formatFileSize(size)
    val formattedDate: String get() = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(lastModified))
    val shortDate: String get() {
        val diff = System.currentTimeMillis() - lastModified
        return when {
            diff < 60_000 -> Strings.justNow
            diff < 3_600_000 -> "${diff / 60_000} ${Strings.minAgo}"
            diff < 7_200_000 -> Strings.hourAgo
            diff < 86_400_000 -> "${diff / 3_600_000} ${Strings.hoursAgo}"
            diff < 172_800_000 -> Strings.yesterday
            diff < 604_800_000 -> "${diff / 86_400_000} ${Strings.daysAgo}"
            diff < 1_209_600_000 -> Strings.weekAgo
            diff < 2_592_000_000 -> "${diff / 604_800_000} ${Strings.weeksAgo}"
            diff < 5_184_000_000 -> Strings.monthAgo
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(lastModified))
        }
    }
}

data class StorageLocation(
    val name: String,
    val icon: String,
    val path: String,
    val isCloud: Boolean = false,
    val color: Color = Blue
)

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 Б"
    val units = arrayOf("B", "KB", "MB", "GB")
    val g = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return "%.1f %s".format(size / Math.pow(1024.0, g.toDouble()), units[g])
}

fun getFileType(ext: String): FileType = when (ext.lowercase()) {
    "jpg","jpeg","png","gif","webp","heic","bmp","svg" -> FileType.IMAGE
    "mp4","avi","mkv","mov","wmv","flv","3gp","webm" -> FileType.VIDEO
    "mp3","wav","flac","aac","ogg","m4a","wma","opus" -> FileType.AUDIO
    "pdf" -> FileType.PDF
    "doc","docx","rtf","odt" -> FileType.DOCUMENT
    "xls","xlsx","csv","tsv","ods" -> FileType.SPREADSHEET
    "ppt","pptx","odp" -> FileType.PRESENTATION
    "zip","rar","7z","tar","gz","tgz","bz2","xz" -> FileType.ARCHIVE
    "kt","java","py","js","ts","tsx","jsx","html","css","xml","json","c","cpp","h","hpp",
    "sh","bash","rb","go","rs","swift","php","sql","dart","scala","lua","r","ps1","bat",
    "yaml","yml","toml","gradle","pro","makefile","dockerfile","gitignore","editorconfig",
    "cfg","ini","conf","properties","env" -> FileType.CODE
    "txt","md","log" -> FileType.TEXT
    else -> FileType.UNKNOWN
}

object DemoData {
    val tags = listOf(
        FileTag("Work", Red), FileTag("Personal", Blue),
        FileTag("Project", Green), FileTag("Important", Orange),
    )

    val storageLocations = listOf(
        StorageLocation(Strings.onMyDevice, "phone_android", "/storage/emulated/0", color = Blue),
        StorageLocation(Strings.downloads, "download", "/storage/emulated/0/Download", color = Green),
        StorageLocation("Google Drive", "cloud", "/gdrive", isCloud = true, color = FolderYellow),
        StorageLocation("Dropbox", "cloud_queue", "/dropbox", isCloud = true, color = Blue),
    )

    val rootFolders = listOf(
        FileItem("Afterparty for WWDC", "/Documents/Afterparty", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.ORANGE, itemCount = 12),
        FileItem("Back Flip", "/Documents/BackFlip", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.BLUE, itemCount = 5),
        FileItem("Awesome Snowball", "/Documents/Snowball", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.BLUE, itemCount = 3),
        FileItem("Brainstorms", "/Documents/Brainstorms", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.GREEN, itemCount = 8),
        FileItem("Camping Trip", "/Documents/Camping", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.GREEN, itemCount = 24),
        FileItem("Color Theory", "/Documents/ColorTheory", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.RED, itemCount = 7),
        FileItem("Copenhagen", "/Documents/Copenhagen", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.PURPLE, itemCount = 15),
        FileItem("Family", "/Documents/Family", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.YELLOW, itemCount = 42),
        FileItem("Finances", "/Documents/Finances", isDirectory = true, type = FileType.FOLDER, folderColor = FolderColor.GREEN, itemCount = 19),
    )

    val recentFiles = listOf(
        FileItem("House Coffee", "/Recent/house_coffee.jpg", size = 2_400_000, type = FileType.IMAGE, extension = "jpg", lastModified = System.currentTimeMillis() - 3_600_000),
        FileItem("Chairs", "/Recent/chairs.png", size = 1_800_000, type = FileType.IMAGE, extension = "png", lastModified = System.currentTimeMillis() - 7_200_000),
        FileItem("Meow", "/Recent/meow.jpg", size = 950_000, type = FileType.IMAGE, extension = "jpg", lastModified = System.currentTimeMillis() - 14_400_000),
        FileItem("Smithsonian", "/Recent/smithsonian.pdf", size = 5_200_000, type = FileType.PDF, extension = "pdf", lastModified = System.currentTimeMillis() - 28_800_000),
        FileItem("Modern Craft Heritage", "/Recent/modern_craft.pdf", size = 76_700_000, type = FileType.PDF, extension = "pdf", lastModified = System.currentTimeMillis() - 86_400_000),
        FileItem("Workshop Newsletter", "/Recent/workshop.pdf", size = 18_700_000, type = FileType.PDF, extension = "pdf", lastModified = System.currentTimeMillis() - 86_400_000),
        FileItem("Alyx", "/Recent/alyx.jpg", size = 3_100_000, type = FileType.IMAGE, extension = "jpg", lastModified = System.currentTimeMillis() - 172_800_000),
        FileItem("Budget 2026", "/Recent/budget.xlsx", size = 450_000, type = FileType.SPREADSHEET, extension = "xlsx", lastModified = System.currentTimeMillis() - 259_200_000),
        FileItem("Presentation Draft", "/Recent/pres.pptx", size = 12_300_000, type = FileType.PRESENTATION, extension = "pptx", lastModified = System.currentTimeMillis() - 345_600_000),
    )

    val downloadsFiles = listOf(
        FileItem("IMG_0918", "/Downloads/IMG_0918.png", size = 444_000, type = FileType.IMAGE, extension = "png", lastModified = System.currentTimeMillis() - 3_600_000),
        FileItem("IPTVplaylist", "/Downloads/IPTVplaylist.m3u", size = 12_000, type = FileType.UNKNOWN, extension = "m3u", lastModified = 1718784000000),
        FileItem("Drawing guide", "/Downloads/posobie.pdf", size = 8_500_000, type = FileType.PDF, extension = "pdf", lastModified = System.currentTimeMillis() - 604_800_000),
        FileItem("Rental agreement 2017", "/Downloads/dogovor.docx", size = 2_100_000, type = FileType.DOCUMENT, extension = "docx", lastModified = System.currentTimeMillis() - 172_800_000),
    )
}
