package com.glassfiles.widget

import android.content.Context
import android.os.Environment
import android.os.StatFs
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

class StorageWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val langName = prefs.getString("app_language", "") ?: ""
        Strings.lang = try { AppLanguage.valueOf(langName) } catch (_: Exception) {
            if (java.util.Locale.getDefault().language == "ru") AppLanguage.RUSSIAN else AppLanguage.ENGLISH
        }
        provideContent { StorageWidgetContent() }
    }
}

private val BgColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFFF2F2F7),
    night = androidx.compose.ui.graphics.Color(0xFF1C1C1E)
)
private val TextColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    night = androidx.compose.ui.graphics.Color.White
)
private val SubtextColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF8E8E93),
    night = androidx.compose.ui.graphics.Color(0xFF8E8E93)
)
private val AccentColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF007AFF),
    night = androidx.compose.ui.graphics.Color(0xFF0A84FF)
)
private val BarBgColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFFE5E5EA),
    night = androidx.compose.ui.graphics.Color(0xFF3A3A3C)
)

@Composable
private fun StorageWidgetContent() {
    val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
    val totalBytes = stat.totalBytes
    val freeBytes = stat.availableBytes
    val usedBytes = totalBytes - freeBytes
    val usedPercent = ((usedBytes.toDouble() / totalBytes) * 100).toInt()

    GlanceTheme {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(BgColor)
                .cornerRadius(20.dp)
                .padding(16.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Glass Files", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColor))
                    Spacer(GlanceModifier.defaultWeight())
                    Text("$usedPercent%", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentColor))
                }
                Spacer(GlanceModifier.height(8.dp))
                LinearProgressIndicator(
                    progress = usedBytes.toFloat() / totalBytes.toFloat(),
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                    color = AccentColor, backgroundColor = BarBgColor
                )
                Spacer(GlanceModifier.height(8.dp))
                Text("${fmtSize(usedBytes)} ${Strings.of} ${fmtSize(totalBytes)}", style = TextStyle(fontSize = 13.sp, color = TextColor))
                Text("${fmtSize(freeBytes)} ${Strings.freeSpace}", style = TextStyle(fontSize = 11.sp, color = SubtextColor))
            }
        }
    }
}

private fun fmtSize(bytes: Long): String = when {
    bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}

class StorageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StorageWidget()
}
