package com.glassfiles.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Adb
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.AppShortcut
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperMode
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PublishedWithChanges
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.Strings
import com.glassfiles.data.github.GHRepo
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.Blue
import com.glassfiles.ui.theme.SurfaceLight
import com.glassfiles.ui.theme.SurfaceWhite
import com.glassfiles.ui.theme.TextPrimary
import com.glassfiles.ui.theme.TextSecondary
import com.glassfiles.ui.theme.TextTertiary
import kotlinx.coroutines.launch

internal data class BuildCategory(
    val title: String,
    val items: List<BuildPreset>
)

internal data class BuildPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val workflowId: String,
    val icon: ImageVector
)

internal data class BuildLaunchState(
    val branch: String,
    val releaseType: String,
    val kernelVersion: String,
    val featureSet: String,
    val ksuCommit: String
)

private val BuildReleaseTypes = listOf("Actions", "Release", "Debug")
private val BuildKernelVersions = listOf("custom", "latest", "stable")
private val BuildFeatureSets = listOf("WKSU+SUSFS+BBG", "clean", "minimal", "full")

private val BuildCatalog = listOf(
    BuildCategory(
        title = "Ядра и система",
        items = listOf(
            BuildPreset("android-kernel-gsi", "Android Kernel (GSI / vanilla)", "Generic kernel build", "android-kernel-gsi.yml", Icons.Rounded.Memory),
            BuildPreset("android-kernel-ksu", "Android Kernel + KernelSU", "Kernel with KernelSU", "android-kernel-ksu.yml", Icons.Rounded.Memory),
            BuildPreset("android-kernel-magisk", "Android Kernel + Magisk", "Kernel with Magisk integration", "android-kernel-magisk.yml", Icons.Rounded.Memory),
            BuildPreset("android-kernel-susfs", "Android Kernel + SUSFS", "Kernel with SUSFS patches", "android-kernel-susfs.yml", Icons.Rounded.Memory),
            BuildPreset("android-kernel-gki", "Android Kernel (GKI / non-GKI)", "GKI and non-GKI variants", "android-kernel-gki.yml", Icons.Rounded.Memory),
            BuildPreset("recovery", "Recovery / Custom Recovery", "TWRP, OrangeFox and similar", "recovery.yml", Icons.Rounded.SystemUpdateAlt),
            BuildPreset("boot-image", "Boot Image (boot.img)", "Boot image generation", "boot-image.yml", Icons.Rounded.Archive)
        )
    ),
    BuildCategory(
        title = "Модули",
        items = listOf(
            BuildPreset("kernelsu-module", "KernelSU Module", "KernelSU module package", "kernelsu-module.yml", Icons.Rounded.Code),
            BuildPreset("magisk-module", "Magisk Module", "Magisk module package", "magisk-module.yml", Icons.Rounded.Code),
            BuildPreset("zygisk-module", "Zygisk Module", "Zygisk extension package", "zygisk-module.yml", Icons.Rounded.Code),
            BuildPreset("lsposed-module", "LSPosed Module", "LSPosed module package", "lsposed-module.yml", Icons.Rounded.Code)
        )
    ),
    BuildCategory(
        title = "Драйверы и графика",
        items = listOf(
            BuildPreset("turnip", "Turnip (Adreno Vulkan Driver)", "Adreno Vulkan driver build", "turnip.yml", Icons.Rounded.Adb),
            BuildPreset("mesa-angle", "Mesa / ANGLE", "Mesa and ANGLE graphics stack", "mesa-angle.yml", Icons.Rounded.DeveloperMode),
            BuildPreset("gpu-driver", "GPU Driver", "Proprietary or open-source GPU drivers", "gpu-driver.yml", Icons.Rounded.DeveloperMode)
        )
    ),
    BuildCategory(
        title = "Приложения",
        items = listOf(
            BuildPreset("android-apk-debug", "Android APK (Debug)", "Debug APK build", "android-apk-debug.yml", Icons.Rounded.Android),
            BuildPreset("android-apk-release", "Android APK (Release)", "Release APK build", "android-apk-release.yml", Icons.Rounded.Android),
            BuildPreset("android-aab", "Android App Bundle (AAB)", "AAB package build", "android-aab.yml", Icons.Rounded.Android),
            BuildPreset("flutter-build", "Flutter Build", "Flutter application build", "flutter-build.yml", Icons.Rounded.Android),
            BuildPreset("react-native-build", "React Native Build", "React Native application build", "react-native-build.yml", Icons.Rounded.Android)
        )
    ),
    BuildCategory(
        title = "Кросс-платформа",
        items = listOf(
            BuildPreset("windows-exe", "Windows EXE / MSI", "Windows desktop artifacts", "windows-exe.yml", Icons.Rounded.AppShortcut),
            BuildPreset("linux-package", "Linux AppImage / DEB / RPM", "Linux packages and bundles", "linux-package.yml", Icons.Rounded.Terminal),
            BuildPreset("macos-dmg", "macOS DMG", "macOS desktop build", "macos-dmg.yml", Icons.Rounded.PhoneIphone),
            BuildPreset("docker-image", "Docker Image", "Container image build", "docker-image.yml", Icons.Rounded.Dns)
        )
    ),
    BuildCategory(
        title = "Автоматизация",
        items = listOf(
            BuildPreset("release-publish", "Release Publish (GitHub Release)", "Publish a release", "release-publish.yml", Icons.Rounded.PublishedWithChanges),
            BuildPreset("changelog-generate", "Changelog Generate", "Generate changelog", "changelog-generate.yml", Icons.Rounded.AutoAwesome),
            BuildPreset("nightly-ci", "Nightly / CI Build", "Nightly and CI build pipeline", "nightly-ci.yml", Icons.Rounded.Build),
            BuildPreset("lint-test-security", "Lint / Test / Security Scan", "Verification workflows", "lint-test-security.yml", Icons.Rounded.Security)
        )
    ),
    BuildCategory(
        title = "Специфические",
        items = listOf(
            BuildPreset("kernelsu-manager-apk", "KernelSU Manager APK", "Manager APK build", "kernelsu-manager-apk.yml", Icons.Rounded.Android),
            BuildPreset("aosp-custom-rom", "AOSP / Custom ROM Build", "AOSP and ROM build", "aosp-custom-rom.yml", Icons.Rounded.Storage),
            BuildPreset("gsi-build", "GSI Build", "Generic System Image build", "gsi-build.yml", Icons.Rounded.Storage),
            BuildPreset("vendor-image", "Vendor Image Build", "Vendor image build", "vendor-image.yml", Icons.Rounded.Storage),
            BuildPreset("dtbo-vendor-boot", "DTBO / Vendor Boot", "DTBO and vendor boot image", "dtbo-vendor-boot.yml", Icons.Rounded.Storage)
        )
    )
)

@Composable
internal fun BuildsScreen(
    repo: GHRepo,
    branches: List<String>,
    onBuildStarted: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val availableBranches = remember(branches, repo.defaultBranch) {
        (listOf(repo.defaultBranch, "main", "dev", "stable") + branches).distinct().filter { it.isNotBlank() }
    }

    var selectedPreset by remember { mutableStateOf<BuildPreset?>(null) }
    var launching by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(SurfaceWhite)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Build, null, tint = Blue)
                    Text("Сборщик", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(
                    "Run GitHub Actions workflows with build parameters via workflow_dispatch and continue tracking progress in Actions.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        BuildCatalog.forEach { category ->
            item {
                Text(
                    category.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                )
            }
            items(category.items) { preset ->
                BuildPresetCard(preset = preset, onClick = { selectedPreset = preset })
            }
        }
    }

    if (selectedPreset != null) {
        BuildWorkflowDialog(
            preset = selectedPreset!!,
            branches = availableBranches.ifEmpty { listOf(repo.defaultBranch.ifBlank { "main" }) },
            launching = launching,
            onDismiss = { if (!launching) selectedPreset = null },
            onLaunch = { state ->
                launching = true
                scope.launch {
                    val ok = GitHubManager.dispatchWorkflow(
                        context = context,
                        owner = repo.owner,
                        repo = repo.name,
                        workflowId = selectedPreset!!.workflowId,
                        ref = state.branch,
                        inputs = mapOf(
                            "release_type" to state.releaseType,
                            "kernel_version" to state.kernelVersion,
                            "feature_set" to state.featureSet,
                            "ksu_commit" to state.ksuCommit
                        )
                    )
                    launching = false
                    if (ok) {
                        Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
                        selectedPreset = null
                        onBuildStarted()
                    } else {
                        Toast.makeText(context, Strings.error, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun BuildPresetCard(
    preset: BuildPreset,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Blue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(preset.icon, null, tint = Blue)
        }
        Column(Modifier.weight(1f)) {
            Text(preset.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(preset.subtitle, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF16A34A).copy(alpha = 0.12f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Run", color = Color(0xFF16A34A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BuildWorkflowDialog(
    preset: BuildPreset,
    branches: List<String>,
    launching: Boolean,
    onDismiss: () -> Unit,
    onLaunch: (BuildLaunchState) -> Unit
) {
    var branch by remember(preset.id) { mutableStateOf(branches.firstOrNull().orEmpty()) }
    var releaseType by remember(preset.id) { mutableStateOf(BuildReleaseTypes.first()) }
    var kernelVersion by remember(preset.id) { mutableStateOf(BuildKernelVersions.first()) }
    var featureSet by remember(preset.id) { mutableStateOf(BuildFeatureSets.first()) }
    var ksuCommit by remember(preset.id) { mutableStateOf("") }

    var branchMenu by remember { mutableStateOf(false) }
    var releaseMenu by remember { mutableStateOf(false) }
    var kernelMenu by remember { mutableStateOf(false) }
    var featureMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(preset.title, fontWeight = FontWeight.Bold)
                Text("Workflow build settings", fontSize = 12.sp, color = TextSecondary)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BuildDropdownField("Branch", branch, { branchMenu = true })
                BuildDropdownField("Release Type", releaseType, { releaseMenu = true })
                BuildDropdownField("Kernel Version to Build", kernelVersion, { kernelMenu = true })
                BuildDropdownField("Feature Set", featureSet, { featureMenu = true })
                OutlinedTextField(
                    value = ksuCommit,
                    onValueChange = { ksuCommit = it },
                    label = { Text("KSU Commit") },
                    placeholder = { Text("optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Workflow: ${preset.workflowId}", fontSize = 11.sp, color = TextTertiary)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onLaunch(
                        BuildLaunchState(
                            branch = branch,
                            releaseType = releaseType,
                            kernelVersion = kernelVersion,
                            featureSet = featureSet,
                            ksuCommit = ksuCommit
                        )
                    )
                },
                enabled = !launching && branch.isNotBlank()
            ) {
                if (launching) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Blue)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color(0xFF16A34A))
                        Text("Run workflow", color = Color(0xFF16A34A), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !launching) {
                Text(Strings.cancel)
            }
        }
    )

    if (branchMenu) SimpleChoiceDialog("Branch", branches, branch, { branch = it; branchMenu = false }) { branchMenu = false }
    if (releaseMenu) SimpleChoiceDialog("Release Type", BuildReleaseTypes, releaseType, { releaseType = it; releaseMenu = false }) { releaseMenu = false }
    if (kernelMenu) SimpleChoiceDialog("Kernel Version to Build", BuildKernelVersions, kernelVersion, { kernelVersion = it; kernelMenu = false }) { kernelMenu = false }
    if (featureMenu) SimpleChoiceDialog("Feature Set", BuildFeatureSets, featureSet, { featureSet = it; featureMenu = false }) { featureMenu = false }
}

@Composable
private fun BuildDropdownField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceLight)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, color = TextPrimary, fontSize = 14.sp)
            Icon(Icons.Rounded.ArrowDropDown, null, tint = Blue)
        }
    }
}

@Composable
private fun SimpleChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (option == selected) Blue.copy(alpha = 0.12f) else SurfaceWhite)
                            .clickable { onPick(option) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(option, color = if (option == selected) Blue else TextPrimary)
                        if (option == selected) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(Blue))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.cancel) }
        }
    )
}
