package com.glassfiles.widget

import android.content.Context
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

class QuickAccessWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val langName = prefs.getString("app_language", "") ?: ""
        Strings.lang = try { AppLanguage.valueOf(langName) } catch (_: Exception) {
            if (java.util.Locale.getDefault().language == "ru") AppLanguage.RUSSIAN else AppLanguage.ENGLISH
        }
        provideContent { QuickAccessWidgetContent() }
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
private val ChipColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFFE5E5EA),
    night = androidx.compose.ui.graphics.Color(0xFF3A3A3C)
)
private val TextColor = ColorProvider(
    day = androidx.compose.ui.graphics.Color(0xFF1C1C1E),
    night = androidx.compose.ui.graphics.Color.White
)

@Composable
private fun QuickAccessWidgetContent() {
    GlanceTheme {
        Box(
            modifier = GlanceModifier.fillMaxSize()
                .background(BgColor)
                .cornerRadius(20.dp)
                .padding(16.dp)
        ) {
            Column(modifier = GlanceModifier.fillMaxSize()) {
                Text("Glass Files", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextColor))
                Spacer(GlanceModifier.height(12.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    QuickFolder("📥", Strings.downloads, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    QuickFolder("📷", Strings.photos, GlanceModifier.defaultWeight())
                }
                Spacer(GlanceModifier.height(8.dp))
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    QuickFolder("🎵", Strings.music, GlanceModifier.defaultWeight())
                    Spacer(GlanceModifier.width(8.dp))
                    QuickFolder("📄", Strings.documents, GlanceModifier.defaultWeight())
                }
                Spacer(GlanceModifier.height(12.dp))

                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ActionChip("🔍", Strings.search)
                    Spacer(GlanceModifier.width(8.dp))
                    ActionChip("🗑", Strings.trash)
                    Spacer(GlanceModifier.width(8.dp))
                    ActionChip("📊", Strings.storage)
                }
            }
        }
    }
}

@Composable
private fun QuickFolder(emoji: String, label: String, modifier: GlanceModifier) {
    Box(
        modifier = modifier.cornerRadius(12.dp).background(CardColor).padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = TextStyle(fontSize = 18.sp))
            Spacer(GlanceModifier.width(8.dp))
            Text(label, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextColor), maxLines = 1)
        }
    }
}

@Composable
private fun ActionChip(emoji: String, label: String) {
    Box(
        modifier = GlanceModifier.cornerRadius(10.dp).background(ChipColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = TextStyle(fontSize = 12.sp))
            Spacer(GlanceModifier.width(4.dp))
            Text(label, style = TextStyle(fontSize = 11.sp, color = TextColor), maxLines = 1)
        }
    }
}

class QuickAccessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAccessWidget()
}
