package com.glassfiles.ui.screens

import android.content.ClipData
import com.glassfiles.data.Strings
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer
import com.glassfiles.ui.theme.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun OcrScreen(onBack: () -> Unit, initialImagePath: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recognizedText by remember { mutableStateOf("") }
    var processing by remember { mutableStateOf(false) }
    var hasImage by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            processing = true
            try {
                val image = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(image).await()
                recognizedText = result.text
                hasImage = true
            } catch (e: Exception) {
                recognizedText = "Error: ${e.message}"
            }
            processing = false
        }
    }

    // Process initial image if provided
    LaunchedEffect(initialImagePath) {
        if (initialImagePath != null) {
            processing = true
            try {
                val bitmap = BitmapFactory.decodeFile(initialImagePath)
                if (bitmap != null) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    val result = recognizer.process(image).await()
                    recognizedText = result.text
                    hasImage = true
                }
            } catch (e: Exception) {
                recognizedText = "Error: ${e.message}"
            }
            processing = false
        }
    }

    val topBg = if (ThemeState.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val circleBg = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val cardBg = if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFFFFFFF)

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(topBg).padding(top = 52.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(circleBg).clickable(onClick = onBack),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp),
                    tint = if (ThemeState.isDark) Color.White else TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text(Strings.recognizeText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.size(36.dp))
        }

        if (processing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Blue)
                    Spacer(Modifier.height(16.dp))
                    Text(Strings.recognizing, color = TextPrimary, fontSize = 16.sp)
                }
            }
        } else if (!hasImage) {
            // No image selected
            Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.DocumentScanner, null, Modifier.size(72.dp), tint = Blue.copy(0.5f))
                Spacer(Modifier.height(20.dp))
                Text(Strings.recognizeFromImage, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(Strings.selectPhoto, color = TextSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(32.dp))
                Button(onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                    Icon(Icons.Rounded.Image, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.choosePhoto)
                }
            }
        } else {
            // Show results
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { imagePicker.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)) {
                        Icon(Icons.Rounded.Image, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(Strings.anotherPhoto)
                    }
                    if (recognizedText.isNotEmpty()) {
                        Button(onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("OCR", recognizedText))
                            Toast.makeText(context, Strings.textCopied, Toast.LENGTH_SHORT).show()
                        }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(Strings.copy)
                        }
                    }
                }

                // Text result
                if (recognizedText.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(Strings.textNotFound, color = TextSecondary, fontSize = 16.sp)
                    }
                } else {
                    Text(Strings.foundText, color = TextSecondary, fontSize = 13.sp)
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cardBg).padding(16.dp)) {
                        SelectionContainer {
                            Text(recognizedText, color = TextPrimary, fontSize = 14.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }
        }
    }
}
