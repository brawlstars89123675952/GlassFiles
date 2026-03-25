package com.glassfiles.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import com.glassfiles.data.FileItem
import com.glassfiles.ui.theme.*

@Composable
fun FileContextMenu(visible: Boolean, item: FileItem?, onDismiss: () -> Unit, onAction: (String) -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.9f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)),
        exit = fadeOut(tween(100)) + scaleOut(targetScale = 0.95f)) {

        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))
            .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            contentAlignment = Alignment.Center) {

            Column(Modifier.widthIn(max = 280.dp)
                .shadow(24.dp, RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.97f), RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .clickable(remember { MutableInteractionSource() }, null) {}) {

                if (item != null) {
                    // Quick actions row
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        QuickBtn(Icons.Rounded.ContentCopy, "Скопи-\nровать") { onAction("copy"); onDismiss() }
                        QuickBtn(Icons.Rounded.DriveFileMove, "Переме-\nстить") { onAction("move"); onDismiss() }
                        QuickBtn(Icons.Rounded.Share, "Поделить-\nся") { onAction("share"); onDismiss() }
                    }
                    MenuDiv()

                    val items = buildMenuItems(item, onAction, onDismiss)
                    items.forEachIndexed { i, m ->
                        MenuRow(m.icon, m.label, if (m.isDestructive) Red else TextPrimary, m.onClick)
                        if (i < items.lastIndex) MenuDiv()
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickBtn(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clickable(remember { MutableInteractionSource() }, null, onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, label, Modifier.size(22.dp), tint = TextPrimary)
        if (label.isNotEmpty()) Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, tint: Color = TextPrimary, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, label, Modifier.size(20.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MenuDiv() { Box(Modifier.fillMaxWidth().height(0.5.dp).background(Color(0x22000000))) }

private data class MI(val icon: ImageVector, val label: String, val isDestructive: Boolean = false, val onClick: () -> Unit)

private fun buildMenuItems(item: FileItem, onAction: (String) -> Unit, onDismiss: () -> Unit): List<MI> {
    val list = mutableListOf<MI>()
    if (!item.isDirectory) {
        list += MI(Icons.Rounded.RemoveRedEye, "Быстро просмотреть") { onAction("quicklook"); onDismiss() }
        list += MI(Icons.Rounded.OpenInNew, "Открыть в приложении") { onAction("openwith"); onDismiss() }
    }
    list += MI(Icons.Outlined.Info, "Свойства") { onAction("info"); onDismiss() }
    list += MI(Icons.Rounded.Edit, "Переименовать") { onAction("rename"); onDismiss() }
    list += MI(Icons.Rounded.Compress, "Сжать") { onAction("compress"); onDismiss() }
    list += MI(Icons.Rounded.FileCopy, "Дублировать") { onAction("duplicate"); onDismiss() }
    list += MI(Icons.Rounded.CreateNewFolder, "Новая папка с 1 объектом") { onAction("newfolder"); onDismiss() }
    list += MI(Icons.Rounded.Label, "Теги...") { onAction("tags"); onDismiss() }

    if (item.path.contains("Download", ignoreCase = true)) {
        list += MI(Icons.Outlined.CloudDownload, "Оставить в загрузках") { onAction("keep"); onDismiss() }
        list += MI(Icons.Rounded.DeleteOutline, "Удалить загрузку", true) { onAction("delete"); onDismiss() }
    } else {
        list += MI(Icons.Rounded.Delete, "Удалить", true) { onAction("delete"); onDismiss() }
    }
    return list
}
