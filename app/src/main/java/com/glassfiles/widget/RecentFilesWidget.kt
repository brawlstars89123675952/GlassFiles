package com.glassfiles.widget

import android.content.Context
import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.color.ColorProvider
import com.glassfiles.MainActivity
import com.glassfiles.data.Strings
import com.glassfiles.data.AppLanguage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecentFilesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val langName = prefs.getString("app_language", "") ?: ""
        Strings.lang = try { AppLanguage.valueOf(langName) } catch (_: Exception) {
            if (java.util.Locale.getDefault().language == "ru") AppLanguage.RUSSIAN else AppLanguage.ENGLISH
        }
        provideContent { RecentFilesWidgetContent() }
    }
}

private val BgColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
    night = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
)
private val CardColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color.White,
    night = androidx.compose.ui.graphics.Color(0xFF2C2C2E)
)
private val TextMain = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    night = androidx.compose.ui.graphics.Color(0xFFE5E5EA)
)
private val TextSub = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF8E8E93),
    night = androidx.compose.ui.graphics.Color(0xFF98989D)
)
private val AccentBlue = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF007AFF),
    night = androidx.compose.ui.graphics.Color(0xFF0A84FF)
)

@Composable
private fun RecentFilesWidgetContent() {
    val context = LocalContext.current
    val recentFiles = getRecentFiles(context, 5)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = GlanceModifier.fillMaxSize().background(BgColor)
            .padding(12.dp).cornerRadius(20.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Header
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Strings.recents,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextMain)
            )
        }

        // Recent files list
        if (recentFiles.isEmpty()) {
            Text(
                if (Strings.lang == AppLanguage.RUSSIAN) "Нет недавних файлов" else "No recent files",
                style = TextStyle(fontSize = 13.sp, color = TextSub),
                modifier = GlanceModifier.padding(top = 8.dp)
            )
        } else {
            recentFiles.forEach { file ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // File icon placeholder
                    Box(
                        modifier = GlanceModifier.size(28.dp).background(AccentBlue).cornerRadius(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            file.extension.take(3).uppercase().ifEmpty { "?" },
                            style = TextStyle(
                                fontSize = 8.sp, fontWeight = FontWeight.Bold,
                                color = ColorProvider(
                                    day = androidx.compose.ui.graphics.Color.White,
                                    night = androidx.compose.ui.graphics.Color.White
                                )
                            )
                        )
                    }
                    Spacer(GlanceModifier.width(8.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            file.name,
                            style = TextStyle(fontSize = 13.sp, color = TextMain, fontWeight = FontWeight.Medium),
                            maxLines = 1
                        )
                        Text(
                            "${fmtSize(file.length())} • ${sdf.format(Date(file.lastModified()))}",
                            style = TextStyle(fontSize = 11.sp, color = TextSub),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun getRecentFiles(context: Context, count: Int): List<File> {
    val root = Environment.getExternalStorageDirectory()
    val dirs = listOf(
        File(root, "Download"),
        File(root, "Documents"),
        File(root, "DCIM"),
        File(root, "Pictures"),
        File(root, "Music")
    )
    return dirs.flatMap { dir ->
        dir.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
    }.sortedByDescending { it.lastModified() }.take(count)
}

private fun fmtSize(b: Long): String = when {
    b < 1024 -> "$b B"
    b < 1024L * 1024 -> "%.0f KB".format(b / 1024.0)
    b < 1024L * 1024 * 1024 -> "%.1f MB".format(b / (1024.0 * 1024))
    else -> "%.1f GB".format(b / (1024.0 * 1024 * 1024))
}

class RecentFilesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecentFilesWidget()
}
