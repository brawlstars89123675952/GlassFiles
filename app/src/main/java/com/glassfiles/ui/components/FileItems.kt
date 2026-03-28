package com.glassfiles.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glassfiles.data.*
import com.glassfiles.ui.theme.*
import java.io.File

@Composable
fun FolderIcon(
    color: Color = Color(0xFF5AC3F8), modifier: Modifier = Modifier,
    size: Dp = 72.dp, overlayIcon: ImageVector? = null, overlayTint: Color = Color.White
) {
    val style = ThemeState.folderStyle
    when (style) {
        com.glassfiles.data.FolderIconStyle.ROUNDED -> {
            // Rounded soft folder — simple rounded rect with icon
            Box(modifier.size(size).background(color.copy(alpha = 0.15f), RoundedCornerShape(size * 0.3f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(size * 0.55f), tint = color)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.3f).offset(y = size * 0.04f), tint = overlayTint.copy(alpha = 0.85f))
            }
        }
        com.glassfiles.data.FolderIconStyle.SHARP -> {
            // Sharp angular folder — square corners
            Box(modifier.size(size).background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.FolderCopy, null, Modifier.size(size * 0.55f), tint = color)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.3f).offset(y = size * 0.04f), tint = overlayTint.copy(alpha = 0.85f))
            }
        }
        com.glassfiles.data.FolderIconStyle.MINIMAL -> {
            // Minimal — just icon, no background
            Box(modifier.size(size), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.FolderOpen, null, Modifier.size(size * 0.65f), tint = color)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.28f).offset(y = size * 0.06f), tint = color.copy(alpha = 0.7f))
            }
        }
        com.glassfiles.data.FolderIconStyle.CIRCLE -> {
            Box(modifier.size(size).background(color.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(size * 0.5f), tint = color)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.25f).offset(y = size * 0.03f), tint = Color.White.copy(alpha = 0.85f))
            }
        }
        com.glassfiles.data.FolderIconStyle.GRADIENT -> {
            val gradBrush = Brush.linearGradient(listOf(color, color.copy(alpha = 0.4f)))
            Box(modifier.size(size).background(gradBrush, RoundedCornerShape(size * 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(size * 0.5f), tint = Color.White)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.25f).offset(y = size * 0.03f), tint = Color.White.copy(alpha = 0.85f))
            }
        }
        com.glassfiles.data.FolderIconStyle.OUTLINED -> {
            Box(modifier.size(size).border(2.dp, color, RoundedCornerShape(size * 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(size * 0.5f), tint = color)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.25f).offset(y = size * 0.03f), tint = color.copy(alpha = 0.7f))
            }
        }
        com.glassfiles.data.FolderIconStyle.FILLED -> {
            Box(modifier.size(size).background(color, RoundedCornerShape(size * 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Folder, null, Modifier.size(size * 0.5f), tint = Color.White)
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.25f).offset(y = size * 0.03f), tint = Color.White.copy(alpha = 0.85f))
            }
        }
        else -> {
            // DEFAULT — original custom Canvas folder
            val topColor = color.copy(red = (color.red + 0.08f).coerceAtMost(1f), green = (color.green + 0.06f).coerceAtMost(1f), blue = (color.blue + 0.04f).coerceAtMost(1f))
            val bottomColor = color.copy(red = (color.red - 0.06f).coerceAtLeast(0f), green = (color.green - 0.04f).coerceAtLeast(0f))
            val shadowColor = color.copy(alpha = 0.3f)
            val lightColor = color.copy(alpha = 0.9f)
            Box(modifier.size(size, size * 0.78f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = this.size.width; val h = this.size.height
                    val earWidth = w * 0.40f; val earHeight = h * 0.18f; val earRadius = earHeight * 0.55f
                    val bodyTop = earHeight * 0.72f; val bodyRadius = w * 0.08f; val earTabRadius = earHeight * 0.4f
                    drawRoundRect(shadowColor, Offset(2f, h * 0.06f), Size(w - 4f, h - bodyTop + bodyTop * 0.15f), CornerRadius(bodyRadius))
                    val earPath = Path().apply {
                        moveTo(bodyRadius, bodyTop); lineTo(bodyRadius, earTabRadius)
                        quadraticTo(bodyRadius, 0f, bodyRadius + earTabRadius, 0f); lineTo(earWidth - earRadius, 0f)
                        quadraticTo(earWidth, 0f, earWidth + earRadius * 0.5f, earHeight * 0.5f)
                        quadraticTo(earWidth + earRadius, earHeight, earWidth + earRadius * 1.5f, bodyTop); close()
                    }
                    drawPath(earPath, Brush.verticalGradient(listOf(topColor, lightColor), 0f, bodyTop))
                    val bodyPath = Path().apply { addRoundRect(RoundRect(Rect(0f, bodyTop, w, h), CornerRadius(bodyRadius))) }
                    drawPath(bodyPath, Brush.verticalGradient(listOf(topColor, bottomColor), bodyTop, h))
                    drawRoundRect(Color.White.copy(alpha = 0.30f), Offset(w * 0.06f, bodyTop + h * 0.02f), Size(w * 0.88f, 1.5f), CornerRadius(1f))
                    drawRoundRect(Color.White.copy(alpha = 0.08f), Offset(w * 0.04f, h * 0.7f), Size(w * 0.92f, h * 0.25f), CornerRadius(bodyRadius * 0.8f))
                }
                if (overlayIcon != null) Icon(overlayIcon, null, Modifier.size(size * 0.38f).offset(y = size * 0.06f), tint = overlayTint.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
fun FileTypeIcon(fileType: FileType, extension: String, modifier: Modifier = Modifier, size: Int = 48) {
    val isDark = ThemeState.isDark
    val (bgColor, icon, iconTint) = remember(fileType, isDark) {
        when (fileType) {
            FileType.IMAGE -> Triple(if (isDark) Color(0xFF1A3A1A) else Color(0xFFE8F5E9), Icons.Rounded.Image, Green)
            FileType.VIDEO -> Triple(if (isDark) Color(0xFF3A1A1A) else Color(0xFFFCE4EC), Icons.Rounded.Videocam, Red)
            FileType.AUDIO -> Triple(if (isDark) Color(0xFF3A2A1A) else Color(0xFFFFF3E0), Icons.Rounded.MusicNote, Orange)
            FileType.PDF -> Triple(if (isDark) Color(0xFF3A1A1E) else Color(0xFFFFEBEE), Icons.Rounded.PictureAsPdf, Red)
            FileType.DOCUMENT -> Triple(if (isDark) Color(0xFF1A2A3A) else Color(0xFFE3F2FD), Icons.Rounded.Description, Blue)
            FileType.SPREADSHEET -> Triple(if (isDark) Color(0xFF1A3A1A) else Color(0xFFE8F5E9), Icons.Rounded.TableChart, Green)
            FileType.PRESENTATION -> Triple(if (isDark) Color(0xFF3A2A1A) else Color(0xFFFFF3E0), Icons.Rounded.Slideshow, Orange)
            FileType.ARCHIVE -> Triple(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F5), Icons.Rounded.FolderZip, TextSecondary)
            FileType.CODE -> Triple(if (isDark) Color(0xFF1A1A3A) else Color(0xFFE8EAF6), Icons.Rounded.Code, Indigo)
            FileType.TEXT -> Triple(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F5), Icons.Rounded.TextSnippet, TextSecondary)
            else -> Triple(if (isDark) Color(0xFF2C2C2E) else Color(0xFFF5F5F5), Icons.Rounded.InsertDriveFile, TextSecondary)
        }
    }
    Box(modifier.size(size.dp).background(bgColor, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size((size * 0.5f).toInt().dp), tint = iconTint)
        if (extension.isNotEmpty()) {
            Box(Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).background(iconTint, RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                Text(extension.uppercase().take(4), fontSize = 7.sp, color = Color.White, lineHeight = 8.sp)
            }
        }
    }
}

/** Thumbnail preview for images/videos, falls back to FileTypeIcon */
@Composable
private fun FileThumbnail(item: FileItem, size: Dp = 72.dp) {
    val isImage = item.type == FileType.IMAGE
    val isVideo = item.type == FileType.VIDEO
    if ((isImage || isVideo) && File(item.path).exists()) {
        Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            AsyncImage(
                model = File(item.path),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isVideo) {
                Box(Modifier.size(28.dp).background(Color.Black.copy(0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp), tint = Color.White)
                }
            }
        }
    } else {
        val bg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
        Box(Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
            FileTypeIcon(item.type, item.extension, size = (size.value * 0.67f).toInt())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderGridItem(item: FileItem, onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    selected: Boolean = false, modifier: Modifier = Modifier) {
    val overlayIcon = when { item.name.contains("Download", true) -> Icons.Rounded.Download; else -> null }
    Column(
        modifier.fillMaxWidth().combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClick = onClick, onLongClick = onLongClick
        ).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box {
            FolderIcon(color = item.folderColor.color, size = 72.dp, overlayIcon = overlayIcon)
            if (selected) Box(Modifier.align(Alignment.TopStart).size(22.dp).background(Blue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White) }
        }
        Text(item.name, fontSize = (ThemeState.fileFontSize - 3).coerceAtLeast(10).sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 15.sp, modifier = Modifier.widthIn(max = 100.dp))
        if (item.itemCount > 0) Text("${item.itemCount} ${Strings.objects}", fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
        else Text(item.shortDate, fontSize = 11.sp, color = TextSecondary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(item: FileItem, onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    selected: Boolean = false, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().combinedClickable(
            interactionSource = remember { MutableInteractionSource() }, indication = null,
            onClick = onClick, onLongClick = onLongClick
        ).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box {
            FileThumbnail(item, 72.dp)
            if (selected) Box(Modifier.align(Alignment.TopStart).size(22.dp).background(Blue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White) }
        }
        Text(item.name, fontSize = (ThemeState.fileFontSize - 3).coerceAtLeast(10).sp, color = TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 15.sp, modifier = Modifier.widthIn(max = 100.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(item.shortDate, fontSize = 11.sp, color = TextSecondary)
            if (item.size > 0) Text(item.formattedSize, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(item: FileItem, onClick: () -> Unit, onLongClick: (() -> Unit)? = null,
    selected: Boolean = false, modifier: Modifier = Modifier, showCloudIcon: Boolean = false) {
    Row(
        modifier.fillMaxWidth()
            .background(if (selected) Blue.copy(0.08f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selected) Box(Modifier.size(22.dp).background(Blue, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = Color.White) }
        if (item.isDirectory) FolderIcon(item.folderColor.color, size = 44.dp)
        else if (item.type == FileType.IMAGE || item.type == FileType.VIDEO) FileThumbnail(item, 44.dp)
        else FileTypeIcon(item.type, item.extension, size = 44)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.name, fontSize = ThemeState.fileFontSize.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.shortDate, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                if (!item.isDirectory && item.size > 0) { Text("–", style = MaterialTheme.typography.bodySmall, color = TextSecondary); Text(item.formattedSize, style = MaterialTheme.typography.bodySmall, color = TextSecondary) }
            }
        }
        if (showCloudIcon && !item.isDownloaded) Icon(Icons.Outlined.CloudDownload, null, Modifier.size(22.dp), tint = Blue)
    }
}
