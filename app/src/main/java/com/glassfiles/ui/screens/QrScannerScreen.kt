package com.glassfiles.ui.screens

import android.Manifest
import com.glassfiles.data.Strings
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.glassfiles.ui.theme.*
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    ) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }
    var scannedResult by remember { mutableStateOf<String?>(null) }
    var scannedType by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    val topBg = if (ThemeState.isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val circleBg = if (ThemeState.isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        // Top bar
        Row(Modifier.fillMaxWidth().background(topBg).padding(top = 52.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(circleBg).clickable(onClick = onBack),
                contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.size(18.dp),
                    tint = if (ThemeState.isDark) Color.White else TextPrimary)
            }
            Spacer(Modifier.weight(1f))
            Text(Strings.qrScanner, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.weight(1f)); Spacer(Modifier.size(36.dp))
        }

        if (!hasCameraPermission) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CameraAlt, null, Modifier.size(64.dp), tint = TextSecondary)
                    Spacer(Modifier.height(16.dp))
                    Text(Strings.needCameraAccess, color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text(Strings.allow) }
                }
            }
        } else if (scannedResult != null) {
            // Result display
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Rounded.QrCode2, null, Modifier.size(64.dp), tint = Green)
                Spacer(Modifier.height(16.dp))
                Text(scannedType, color = TextSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Text(scannedResult!!, color = TextPrimary, fontSize = 16.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)).padding(16.dp))
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        clip.setPrimaryClip(ClipData.newPlainText("QR", scannedResult))
                        Toast.makeText(context, Strings.copied, Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(Strings.copy) }
                    if (scannedResult!!.startsWith("http")) {
                        Button(onClick = {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(scannedResult))) } catch (_: Exception) {}
                        }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text(Strings.open)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { scannedResult = null }) { Text(Strings.scanMore) }
            }
        } else {
            // Camera preview
            CameraPreview(onBarcodeScanned = { value, type ->
                scannedResult = value
                scannedType = type
            })
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreview(onBarcodeScanned: (String, String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned by remember { mutableStateOf(false) }

    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val executor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { analysis ->
                    analysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null && !scanned) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            val scanner = BarcodeScanning.getClient()
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty() && !scanned) {
                                        scanned = true
                                        val barcode = barcodes.first()
                                        val value = barcode.rawValue ?: ""
                                        val type = when (barcode.valueType) {
                                            Barcode.TYPE_URL -> "URL"
                                            Barcode.TYPE_WIFI -> "WiFi"
                                            Barcode.TYPE_EMAIL -> "Email"
                                            Barcode.TYPE_PHONE -> "Phone"
                                            Barcode.TYPE_SMS -> "SMS"
                                            Barcode.TYPE_GEO -> "Location"
                                            Barcode.TYPE_CONTACT_INFO -> "Contact"
                                            else -> "Text"
                                        }
                                        onBarcodeScanned(value, type)
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analyzer)
            } catch (_: Exception) {}
        }, ContextCompat.getMainExecutor(ctx))

        previewView
    }, modifier = Modifier.fillMaxSize())

    // Overlay frame
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(250.dp).border(2.dp, Blue.copy(0.6f), RoundedCornerShape(16.dp)))
        Text(Strings.pointCamera, color = Color.White.copy(0.7f), fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp))
    }
}
