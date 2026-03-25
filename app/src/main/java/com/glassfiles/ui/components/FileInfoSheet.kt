package com.glassfiles.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.glassfiles.data.*
import com.glassfiles.ui.theme.*

@Composable
fun FileInfoSheet(visible: Boolean, item: FileItem?, onDismiss: () -> Unit) {
    AnimatedVisibility(visible && item != null,
        enter = fadeIn() + slideInVertically { it / 3 },
        exit = fadeOut() + slideOutVertically { it / 3 }) {

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center) {

            if (item != null) {
                Column(Modifier.widthIn(max = 340.dp).fillMaxHeight(0.85f)
                    .shadow(32.dp, RoundedCornerShape(20.dp))
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(remember { MutableInteractionSource() }, null) {}) {

                    // Header
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.size(28.dp))
                        Text("Сведения", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Box(Modifier.size(28.dp).background(Blue, CircleShape).clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, "Закрыть", Modifier.size(18.dp), tint = Color.White)
                        }
                    }

                    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        // Preview
                        Box(Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF2F2F7)), contentAlignment = Alignment.Center) {
                            if (item.isDirectory) FolderIcon(item.folderColor.color, size = 80.dp)
                            else FileTypeIcon(item.type, item.extension, size = 72)
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(item.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("${item.extension.uppercase()} файл", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.background(Blue, RoundedCornerShape(10.dp)).clickable { }
                            .padding(horizontal = 20.dp, vertical = 8.dp)) {
                            Text("Открыть", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }

                        Spacer(Modifier.height(20.dp))
                        Text("Всегда открывать\nв приложении", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                        Spacer(Modifier.height(8.dp))
                        AppRow(Icons.Rounded.Preview, "Просмотр", true) {}
                        AppRow(Icons.Rounded.AutoFixHigh, "Snapseed", false) {}
                        AppRow(Icons.Rounded.Brush, "VSCO", false) {}

                        Spacer(Modifier.height(16.dp))
                        Text("Информация", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))

                        InfoRow("Тип", item.extension.uppercase())
                        InfoRow("Размер", item.formattedSize)
                        InfoRow("Создано", item.formattedDate)
                        InfoRow("Изменено", item.formattedDate)
                        InfoRow("Последнее открытие", item.formattedDate)
                        if (item.type == FileType.IMAGE) InfoRow("Разрешение", "1800 × 4000")
                        InfoRow("Где", "На устройстве > Загрузки")

                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Теги", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("Добавить теги", style = MaterialTheme.typography.bodySmall, color = Blue)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(iconVector: ImageVector, appName: String, isDefault: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).background(Blue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(iconVector, appName, Modifier.size(20.dp), tint = Blue)
        }
        Column(Modifier.weight(1f)) {
            Text(appName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            if (isDefault) Text("По умолчанию", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        if (isDefault) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(20.dp), tint = Blue)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(0.45f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(0.55f))
    }
}
