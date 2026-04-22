package com.glassfiles.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glassfiles.data.github.GHCollaborator
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.*
import kotlinx.coroutines.launch

private val COLLABORATOR_PERMISSIONS = listOf(
    "pull" to "Read",
    "triage" to "Triage",
    "push" to "Write",
    "maintain" to "Maintain",
    "admin" to "Admin"
)

@Composable
internal fun CollaboratorsScreen(
    repoOwner: String,
    repoName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var collaborators by remember { mutableStateOf<List<GHCollaborator>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var newUsername by remember { mutableStateOf("") }
    var selectedPermission by remember { mutableStateOf("push") }
    var showAddDialog by remember { mutableStateOf(false) }
    var userToRemove by remember { mutableStateOf<GHCollaborator?>(null) }
    var userToEdit by remember { mutableStateOf<GHCollaborator?>(null) }

    fun loadCollaborators() {
        loading = true
        scope.launch {
            collaborators = GitHubManager.getCollaborators(context, repoOwner, repoName)
            loading = false
        }
    }

    LaunchedEffect(repoOwner, repoName) { loadCollaborators() }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = "Collaborators",
            subtitle = "$repoOwner/$repoName",
            onBack = onBack,
            actions = {
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Rounded.PersonAdd, null, Modifier.size(20.dp), tint = Blue)
                }
            }
        )

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(collaborators) { collaborator ->
                    CollaboratorCard(
                        collaborator = collaborator,
                        onPermissionChange = { userToEdit = collaborator },
                        onRemove = { userToRemove = collaborator }
                    )
                }

                if (collaborators.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No collaborators yet", fontSize = 14.sp, color = TextTertiary)
                        }
                    }
                }
            }
        }
    }

    // Add collaborator dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SurfaceWhite,
            title = { Text("Add Collaborator", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Permission level:", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        COLLABORATOR_PERMISSIONS.forEach { (perm, label) ->
                            PermissionChip(
                                label = label,
                                selected = perm == selectedPermission
                            ) {
                                selectedPermission = perm
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUsername.isNotBlank()) {
                            scope.launch {
                                val ok = GitHubManager.addCollaborator(context, repoOwner, repoName, newUsername, selectedPermission)
                                Toast.makeText(context, if (ok) "Invitation sent" else "Failed", Toast.LENGTH_SHORT).show()
                                if (ok) {
                                    newUsername = ""
                                    showAddDialog = false
                                    loadCollaborators()
                                }
                            }
                        }
                    }
                ) {
                    Text("Add", color = Blue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Remove confirmation
    if (userToRemove != null) {
        AlertDialog(
            onDismissRequest = { userToRemove = null },
            containerColor = SurfaceWhite,
            title = { Text("Remove Collaborator?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Text("Remove ${userToRemove!!.login} from this repository?", fontSize = 14.sp, color = TextSecondary)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = GitHubManager.removeCollaborator(context, repoOwner, repoName, userToRemove!!.login)
                            Toast.makeText(context, if (ok) "Removed" else "Failed", Toast.LENGTH_SHORT).show()
                            userToRemove = null
                            loadCollaborators()
                        }
                    }
                ) {
                    Text("Remove", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { userToRemove = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit permission dialog
    if (userToEdit != null) {
        var editPermission by remember(userToEdit) { mutableStateOf(userToEdit!!.role) }
        AlertDialog(
            onDismissRequest = { userToEdit = null },
            containerColor = SurfaceWhite,
            title = { Text("Change Permission", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${userToEdit!!.login}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Select permission level:", fontSize = 12.sp, color = TextSecondary)
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        COLLABORATOR_PERMISSIONS.forEach { (perm, label) ->
                            PermissionChip(
                                label = label,
                                selected = perm == editPermission
                            ) {
                                editPermission = perm
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val ok = GitHubManager.updateCollaboratorPermission(context, repoOwner, repoName, userToEdit!!.login, editPermission)
                            Toast.makeText(context, if (ok) "Updated" else "Failed", Toast.LENGTH_SHORT).show()
                            userToEdit = null
                            loadCollaborators()
                        }
                    }
                ) {
                    Text("Save", color = Blue)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun CollaboratorCard(
    collaborator: GHCollaborator,
    onPermissionChange: () -> Unit,
    onRemove: () -> Unit
) {
    val roleColor = when (collaborator.role) {
        "admin" -> Color(0xFFFF3B30)
        "maintain" -> Color(0xFFFF9F0A)
        "write" -> Color(0xFF34C759)
        "triage" -> Blue
        else -> TextSecondary
    }

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            collaborator.avatarUrl,
            collaborator.login,
            Modifier.size(40.dp).clip(CircleShape)
        )
        Column(Modifier.weight(1f)) {
            Text(collaborator.login, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(roleColor))
                Text(
                    collaborator.role.replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    color = roleColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        IconButton(onClick = onPermissionChange) {
            Icon(Icons.Rounded.Edit, null, Modifier.size(18.dp), tint = Blue)
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.PersonRemove, null, Modifier.size(18.dp), tint = Color(0xFFFF3B30))
        }
    }
}

@Composable
private fun PermissionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Blue.copy(0.15f) else SurfaceLight)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Blue else TextPrimary
        )
    }
}
