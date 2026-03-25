package com.glassfiles.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.FileItem
import com.glassfiles.data.Strings
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilePropertiesSheet(file: FileItem?, onDismiss: () -> Unit) {
    if (file == null) return
    val f = remember(file) { File(file.path) }
    var folderSize by remember { mutableStateOf<Long?>(null) }
    var fileCount by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var md5 by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(file) {
        scope.launch(Dispatchers.IO) {
            if (f.isDirectory) {
                var size = 0L; var dirs = 0; var files = 0
                f.walkTopDown().forEach { if (it.isFile) { size += it.length(); files++ } else if (it != f) dirs++ }
                withContext(Dispatchers.Main) { folderSize = size; fileCount = Pair(dirs, files) }
            } else if (f.length() < 50_000_000) {
                try {
                    val digest = MessageDigest.getInstance("MD5")
                    f.inputStream().use { input -> val buf = ByteArray(8192); var n: Int; while (input.read(buf).also { n = it } != -1) digest.update(buf, 0, n) }
                    withContext(Dispatchers.Main) { md5 = digest.digest().joinToString("") { "%02x".format(it) } }
                } catch (_: Exception) {}
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceWhite) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (file.isDirectory) FolderIcon(file.folderColor.color, size = 56.dp) else FileTypeIcon(file.type, file.extension, size = 56)
                Column { Text(file.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(file.extension.uppercase().ifEmpty { if (file.isDirectory) Strings.propFolder else Strings.propFile }, fontSize = 13.sp, color = TextSecondary) }
            }
            Spacer(Modifier.height(20.dp))
            val sdf = SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.getDefault())
            val props = buildList {
                add(Triple(Icons.Rounded.Description, Strings.propType, if (file.isDirectory) Strings.propFolder else file.extension.uppercase().ifEmpty { "Unknown" }))
                add(Triple(Icons.Rounded.Storage, Strings.propSize, if (file.isDirectory) { if (folderSize != null) fmtSz(folderSize!!) else "..." } else fmtSz(file.size)))
                if (fileCount != null) add(Triple(Icons.Rounded.FolderOpen, "Contents", "${fileCount!!.first} папок, ${fileCount!!.second} файлов"))
                add(Triple(Icons.Rounded.CalendarToday, Strings.propModified, sdf.format(Date(f.lastModified()))))
                add(Triple(Icons.Rounded.LocationOn, Strings.propPath, file.path))
                add(Triple(Icons.Rounded.Lock, "Permissions", "${if (f.canRead()) "R" else "-"}${if (f.canWrite()) "W" else "-"}${if (f.canExecute()) "X" else "-"}"))
                if (!file.isDirectory && md5 != null) add(Triple(Icons.Rounded.Fingerprint, "MD5", md5!!))
            }
            props.forEach { (icon, label, value) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(icon, null, Modifier.size(20.dp), tint = Blue)
                    Column { Text(label, fontSize = 12.sp, color = TextSecondary); Text(value, fontSize = 14.sp, color = TextPrimary, lineHeight = 18.sp) }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

private fun fmtSz(bytes: Long): String = when {
    bytes < 1024 -> "$bytes Б"; bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} КБ"
    bytes < 1024L * 1024 * 1024 -> "${"%.1f".format(bytes / (1024.0 * 1024))} МБ"
    else -> "${"%.2f".format(bytes / (1024.0 * 1024 * 1024))} ГБ"
}
