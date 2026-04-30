package com.glassfiles.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.*
import com.glassfiles.data.ai.AiAgentApprovalPrefs
import com.glassfiles.ui.screens.ai.terminal.YoloModeConfirmDialog
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    appSettings: AppSettings,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableIntStateOf(0) } // 0 = language, 1 = permission, 2 = agent mode
    var yoloConfirmed by remember { mutableStateOf(false) }
    var showYoloConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        yoloConfirmed = AiAgentApprovalPrefs.isYoloModeConfirmed(context)
    }

    fun completeWithManualMode() {
        AiAgentApprovalPrefs.setYoloMode(context, false)
        AiAgentApprovalPrefs.setAutoApproveReads(context, false)
        AiAgentApprovalPrefs.setAutoApproveEdits(context, false)
        AiAgentApprovalPrefs.setAutoApproveWrites(context, false)
        AiAgentApprovalPrefs.setAutoApproveCommits(context, false)
        appSettings.completeOnboarding()
        onComplete()
    }

    fun completeWithAssistedMode() {
        AiAgentApprovalPrefs.setYoloMode(context, false)
        AiAgentApprovalPrefs.setAutoApproveReads(context, true)
        AiAgentApprovalPrefs.setAutoApproveEdits(context, false)
        AiAgentApprovalPrefs.setAutoApproveWrites(context, false)
        AiAgentApprovalPrefs.setAutoApproveCommits(context, false)
        appSettings.completeOnboarding()
        onComplete()
    }

    fun completeWithYoloMode() {
        AiAgentApprovalPrefs.setYoloMode(context, true)
        AiAgentApprovalPrefs.setAutoApproveReads(context, true)
        AiAgentApprovalPrefs.setAutoApproveEdits(context, true)
        AiAgentApprovalPrefs.setAutoApproveWrites(context, true)
        AiAgentApprovalPrefs.setAutoApproveCommits(context, true)
        Toast.makeText(
            context,
            "YOLO mode enabled. Agent will not ask for most actions.",
            Toast.LENGTH_LONG,
        ).show()
        scope.launch {
            AiAgentApprovalPrefs.setYoloModeConfirmed(context, true)
            yoloConfirmed = true
            appSettings.completeOnboarding()
            onComplete()
        }
    }

    Box(Modifier.fillMaxSize().background(SurfaceLight)) {
        AnimatedContent(step, transitionSpec = {
            (fadeIn(tween(300)) + slideInHorizontally(tween(350)) { it / 3 }) togetherWith
            (fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { -it / 3 })
        }, label = "onboard") { currentStep ->
            when (currentStep) {
                0 -> LanguageStep(appSettings) { step = 1 }
                1 -> PermissionStep(hasPermission, onRequestPermission) {
                    step = 2
                }
                2 -> AgentModeStep(
                    onManual = ::completeWithManualMode,
                    onAssisted = ::completeWithAssistedMode,
                    onYolo = { showYoloConfirm = true },
                )
            }
        }

        // Progress dots
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(3) { i ->
                Box(Modifier.size(if (i == step) 24.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i == step) Blue else TextTertiary.copy(0.3f)))
            }
        }

        if (showYoloConfirm) {
            YoloModeConfirmDialog(
                previouslyConfirmed = yoloConfirmed,
                onEnable = {
                    showYoloConfirm = false
                    completeWithYoloMode()
                },
                onDismiss = { showYoloConfirm = false },
            )
        }
    }
}

@Composable
private fun LanguageStep(appSettings: AppSettings, onNext: () -> Unit) {
    var selectedLang by remember { mutableStateOf(appSettings.appLanguage) }

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Folder, null, Modifier.size(72.dp), tint = Blue)
        Spacer(Modifier.height(16.dp))
        Text("Glass Files", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(40.dp))

        Text(Strings.chooseLanguage, fontSize = 16.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))

        AppLanguage.entries.forEach { lang ->
            val isSelected = lang == selectedLang
            val bg = if (isSelected) Blue.copy(0.12f) else if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
            val border = if (isSelected) Blue.copy(0.5f) else Color.Transparent

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(1.5.dp, border, RoundedCornerShape(14.dp))
                    .clickable {
                        selectedLang = lang
                        appSettings.changeLanguage(lang)
                    }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(lang.flag, fontSize = 28.sp)
                Text(lang.label, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                if (isSelected) Icon(Icons.Rounded.CheckCircle, null, Modifier.size(24.dp), tint = Blue)
            }
        }

        Spacer(Modifier.height(40.dp))

        Button(onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Blue)
        ) {
            Text(Strings.continueBtn, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun PermissionStep(hasPermission: Boolean, onRequest: () -> Unit, onComplete: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(80.dp).background(Blue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Storage, null, Modifier.size(40.dp), tint = Blue)
        }
        Spacer(Modifier.height(24.dp))
        Text(Strings.storageAccess, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(Strings.storageAccessDesc, fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))

        AnimatedContent(hasPermission, transitionSpec = {
            (fadeIn(tween(500)) + scaleIn(tween(400), initialScale = 0.8f)) togetherWith fadeOut(tween(300))
        }, label = "perm") { granted ->
            if (granted) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.CheckCircle, null, Modifier.size(48.dp), tint = Color(0xFF34C759))
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onComplete,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Blue)
                    ) {
                        Text(Strings.getStarted, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            } else {
                Button(onClick = onRequest,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue)
                ) {
                    Icon(Icons.Rounded.Lock, null, Modifier.size(20.dp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(Strings.grantAccess, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AgentModeStep(
    onManual: () -> Unit,
    onAssisted: () -> Unit,
    onYolo: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(80.dp).background(Blue.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.SmartToy, null, Modifier.size(40.dp), tint = Blue)
        }
        Spacer(Modifier.height(24.dp))
        Text("AI Agent Mode", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text("Choose how much the agent can do automatically.", fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))

        AgentModeChoice(
            title = "MANUAL MODE",
            subtitle = "Agent asks before tool actions.",
            onClick = onManual,
        )
        AgentModeChoice(
            title = "ASSISTED MODE",
            subtitle = "Reads can run automatically; writes still ask.",
            onClick = onAssisted,
        )
        AgentModeChoice(
            title = "FULL AGENT MODE / YOLO",
            subtitle = "Most actions run without confirmation.",
            warning = true,
            onClick = onYolo,
        )
    }
}

@Composable
private fun AgentModeChoice(
    title: String,
    subtitle: String,
    warning: Boolean = false,
    onClick: () -> Unit,
) {
    val border = if (warning) Color(0xFFE5C07B) else Blue.copy(0.35f)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (ThemeState.isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7))
            .border(1.5.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            if (warning) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
            null,
            Modifier.size(24.dp),
            tint = if (warning) Color(0xFFE5C07B) else Blue,
        )
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, fontSize = 13.sp, color = TextSecondary)
        }
    }
}
