package com.glassfiles.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glassfiles.data.github.GHProject
import com.glassfiles.data.github.GHProjectCard
import com.glassfiles.data.github.GHProjectColumn
import com.glassfiles.data.github.GHProjectV2
import com.glassfiles.data.github.GHRepo
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.Blue
import com.glassfiles.ui.theme.SurfaceLight
import com.glassfiles.ui.theme.SurfaceWhite
import com.glassfiles.ui.theme.TextPrimary
import com.glassfiles.ui.theme.TextSecondary
import com.glassfiles.ui.theme.TextTertiary
import kotlinx.coroutines.launch

private enum class ProjectsKind { CLASSIC, V2 }

@Composable
internal fun ProjectsTab(repo: GHRepo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var classicProjects by remember { mutableStateOf<List<GHProject>>(emptyList()) }
    var v2Projects by remember { mutableStateOf<List<GHProjectV2>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selectedKind by remember { mutableStateOf(ProjectsKind.CLASSIC) }
    var query by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<GHProject?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun loadProjects() {
        loading = true
        scope.launch {
            classicProjects = GitHubManager.getRepoProjects(context, repo.owner, repo.name)
            v2Projects = GitHubManager.getRepoProjectsV2(context, repo.owner, repo.name)
            loading = false
        }
    }

    LaunchedEffect(repo.owner, repo.name) { loadProjects() }

    selectedProject?.let { project ->
        ClassicProjectDetail(
            project = project,
            onBack = { selectedProject = null },
            onDeleted = {
                selectedProject = null
                loadProjects()
            },
            onChanged = { updated ->
                selectedProject = updated
                classicProjects = classicProjects.map { if (it.id == updated.id) updated else it }
            }
        )
        return
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Blue, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
        }
        return
    }

    val visibleClassic = classicProjects.filter {
        query.isBlank() || it.name.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
    }
    val visibleV2 = v2Projects.filter {
        query.isBlank() || it.title.contains(query, ignoreCase = true) || it.shortDescription.contains(query, ignoreCase = true)
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ProjectsSummaryCard(classicProjects, v2Projects) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search projects") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = TextSecondary) }
                )
                IconButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(22.dp), tint = Blue)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ProjectChip("Classic ${classicProjects.size}", selectedKind == ProjectsKind.CLASSIC) { selectedKind = ProjectsKind.CLASSIC }
                ProjectChip("V2 ${v2Projects.size}", selectedKind == ProjectsKind.V2) { selectedKind = ProjectsKind.V2 }
            }
        }
        when (selectedKind) {
            ProjectsKind.CLASSIC -> {
                items(visibleClassic) { project -> ClassicProjectCard(project) { selectedProject = project } }
                if (visibleClassic.isEmpty()) item { EmptyProjectsCard(if (classicProjects.isEmpty()) "No classic projects returned" else "No matching classic projects") }
            }
            ProjectsKind.V2 -> {
                items(visibleV2) { project -> ProjectV2Card(project) }
                if (visibleV2.isEmpty()) item { EmptyProjectsCard(if (v2Projects.isEmpty()) "No Projects V2 returned" else "No matching Projects V2") }
            }
        }
    }

    if (showCreateDialog) {
        ProjectEditorDialog(
            title = "New Classic Project",
            initialName = "",
            initialBody = "",
            initialState = "open",
            showState = false,
            confirmLabel = "Create",
            onDismiss = { showCreateDialog = false },
            onSave = { name, body, _ ->
                scope.launch {
                    val project = GitHubManager.createRepoProject(context, repo.owner, repo.name, name, body)
                    Toast.makeText(context, if (project != null) "Project created" else "Failed", Toast.LENGTH_SHORT).show()
                    if (project != null) {
                        showCreateDialog = false
                        classicProjects = listOf(project) + classicProjects
                    }
                }
            }
        )
    }
}

@Composable
private fun ProjectsSummaryCard(classicProjects: List<GHProject>, v2Projects: List<GHProjectV2>) {
    val openClassic = classicProjects.count { it.state == "open" }
    val openV2 = v2Projects.count { !it.closed }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Dashboard, null, Modifier.size(18.dp), tint = Blue)
            Column(Modifier.weight(1f)) {
                Text("Projects", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("${classicProjects.size} classic - ${v2Projects.size} v2", fontSize = 11.sp, color = TextTertiary)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CountPill("Open classic", openClassic, Blue)
            CountPill("Open V2", openV2, Color(0xFF34C759))
            CountPill("Closed", classicProjects.size - openClassic + v2Projects.size - openV2, TextSecondary)
        }
    }
}

@Composable
private fun ClassicProjectCard(project: GHProject, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.ViewColumn, null, Modifier.size(20.dp), tint = if (project.state == "open") Blue else TextSecondary)
            Text(project.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            CountPill(project.state, 0, if (project.state == "open") Color(0xFF34C759) else TextSecondary, showCount = false)
        }
        if (project.body.isNotBlank()) Text(project.body, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("#${project.number}", fontSize = 11.sp, color = TextTertiary)
            Text(project.updatedAt.take(10), fontSize = 11.sp, color = TextTertiary)
            if (project.creator.isNotBlank()) Text(project.creator, fontSize = 11.sp, color = Blue)
        }
    }
}

@Composable
private fun ProjectV2Card(project: GHProjectV2) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Dashboard, null, Modifier.size(20.dp), tint = if (!project.closed) Blue else TextSecondary)
            Text(project.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (project.url.isNotBlank()) {
                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(project.url))) }) {
                    Icon(Icons.Rounded.Language, null, Modifier.size(18.dp), tint = TextSecondary)
                }
            }
        }
        if (project.shortDescription.isNotBlank()) Text(project.shortDescription, fontSize = 12.sp, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CountPill("${project.itemsCount} items", 0, Blue, showCount = false)
            CountPill(if (project.closed) "Closed" else "Open", 0, if (project.closed) TextSecondary else Color(0xFF34C759), showCount = false)
            CountPill(if (project.isPublic) "Public" else "Private", 0, TextSecondary, showCount = false)
        }
    }
}

@Composable
private fun ClassicProjectDetail(
    project: GHProject,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    onChanged: (GHProject) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentProject by remember(project.id) { mutableStateOf(project) }
    var columns by remember(project.id) { mutableStateOf<List<GHProjectColumn>>(emptyList()) }
    val cardsByColumn = remember(project.id) { mutableStateMapOf<Long, List<GHProjectCard>>() }
    var loading by remember(project.id) { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showColumnDialog by remember { mutableStateOf(false) }
    var cardTargetColumn by remember { mutableStateOf<GHProjectColumn?>(null) }
    var moveTarget by remember { mutableStateOf<Pair<GHProjectCard, GHProjectColumn>?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }

    fun loadProject() {
        loading = true
        scope.launch {
            GitHubManager.getProject(context, currentProject.id)?.let {
                currentProject = it
                onChanged(it)
            }
            columns = GitHubManager.getProjectColumns(context, currentProject.id)
            cardsByColumn.clear()
            columns.forEach { column ->
                cardsByColumn[column.id] = GitHubManager.getProjectCards(context, column.id)
            }
            loading = false
        }
    }

    LaunchedEffect(project.id) { loadProject() }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        GHTopBar(
            title = currentProject.name,
            subtitle = "Classic project #${currentProject.number}",
            onBack = onBack,
            actions = {
                if (currentProject.htmlUrl.isNotBlank()) {
                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentProject.htmlUrl))) }) {
                        Icon(Icons.Rounded.Language, null, Modifier.size(20.dp), tint = TextSecondary)
                    }
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(20.dp), tint = Blue)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Rounded.Delete, null, Modifier.size(20.dp), tint = Color(0xFFFF3B30))
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ProjectDetailSummary(currentProject, columns, cardsByColumn.values.sumOf { it.size }) }
                item {
                    Button(onClick = { showColumnDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                        Text("Add column")
                    }
                }
                items(columns) { column ->
                    ProjectColumnCard(
                        column = column,
                        cards = cardsByColumn[column.id].orEmpty(),
                        allColumns = columns,
                        onAddCard = { cardTargetColumn = column },
                        onMoveCard = { card -> moveTarget = card to column },
                        onDeleteCard = { card ->
                            actionInFlight = true
                            scope.launch {
                                val ok = GitHubManager.deleteProjectCard(context, card.id)
                                Toast.makeText(context, if (ok) "Card deleted" else "Failed", Toast.LENGTH_SHORT).show()
                                actionInFlight = false
                                if (ok) loadProject()
                            }
                        }
                    )
                }
                if (columns.isEmpty()) item { EmptyProjectsCard("No columns yet") }
            }
        }
    }

    if (showEditDialog) {
        ProjectEditorDialog(
            title = "Edit Project",
            initialName = currentProject.name,
            initialBody = currentProject.body,
            initialState = currentProject.state,
            showState = true,
            confirmLabel = "Save",
            onDismiss = { showEditDialog = false },
            onSave = { name, body, state ->
                actionInFlight = true
                scope.launch {
                    val ok = GitHubManager.updateProject(context, currentProject.id, name, body, state)
                    Toast.makeText(context, if (ok) "Project updated" else "Failed", Toast.LENGTH_SHORT).show()
                    actionInFlight = false
                    if (ok) {
                        showEditDialog = false
                        loadProject()
                    }
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = SurfaceWhite,
            title = { Text("Delete Project?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text("Delete ${currentProject.name} and its cards?", fontSize = 14.sp, color = TextSecondary) },
            confirmButton = {
                TextButton(
                    enabled = !actionInFlight,
                    onClick = {
                        actionInFlight = true
                        scope.launch {
                            val ok = GitHubManager.deleteProject(context, currentProject.id)
                            Toast.makeText(context, if (ok) "Deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            actionInFlight = false
                            showDeleteDialog = false
                            if (ok) onDeleted()
                        }
                    }
                ) { Text("Delete", color = Color(0xFFFF3B30)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel", color = TextSecondary) } }
        )
    }

    if (showColumnDialog) {
        TextInputDialog(
            title = "New Column",
            label = "Column name",
            confirmLabel = "Create",
            onDismiss = { showColumnDialog = false },
            onConfirm = { name ->
                scope.launch {
                    val column = GitHubManager.createProjectColumn(context, currentProject.id, name)
                    Toast.makeText(context, if (column != null) "Column created" else "Failed", Toast.LENGTH_SHORT).show()
                    if (column != null) {
                        showColumnDialog = false
                        loadProject()
                    }
                }
            }
        )
    }

    cardTargetColumn?.let { column ->
        TextInputDialog(
            title = "New Card",
            label = "Note",
            confirmLabel = "Create",
            minLines = 4,
            onDismiss = { cardTargetColumn = null },
            onConfirm = { note ->
                scope.launch {
                    val card = GitHubManager.createProjectCard(context, column.id, note)
                    Toast.makeText(context, if (card != null) "Card created" else "Failed", Toast.LENGTH_SHORT).show()
                    if (card != null) {
                        cardTargetColumn = null
                        loadProject()
                    }
                }
            }
        )
    }

    moveTarget?.let { (card, fromColumn) ->
        MoveCardDialog(
            card = card,
            fromColumn = fromColumn,
            columns = columns,
            onDismiss = { moveTarget = null },
            onMove = { targetColumn ->
                scope.launch {
                    val ok = GitHubManager.moveProjectCard(context, card.id, "bottom", targetColumn.id)
                    Toast.makeText(context, if (ok) "Card moved" else "Failed", Toast.LENGTH_SHORT).show()
                    if (ok) {
                        moveTarget = null
                        loadProject()
                    }
                }
            }
        )
    }
}

@Composable
private fun ProjectDetailSummary(project: GHProject, columns: List<GHProjectColumn>, cards: Int) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.ViewColumn, null, Modifier.size(20.dp), tint = Blue)
            Column(Modifier.weight(1f)) {
                Text(project.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(project.updatedAt.take(10), fontSize = 11.sp, color = TextTertiary)
            }
            CountPill(project.state, 0, if (project.state == "open") Color(0xFF34C759) else TextSecondary, showCount = false)
        }
        if (project.body.isNotBlank()) Text(project.body, fontSize = 13.sp, color = TextSecondary)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CountPill("Columns", columns.size, Blue)
            CountPill("Cards", cards, Color(0xFF34C759))
        }
    }
}

@Composable
private fun ProjectColumnCard(
    column: GHProjectColumn,
    cards: List<GHProjectCard>,
    allColumns: List<GHProjectColumn>,
    onAddCard: () -> Unit,
    onMoveCard: (GHProjectCard) -> Unit,
    onDeleteCard: (GHProjectCard) -> Unit
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.ViewColumn, null, Modifier.size(18.dp), tint = Blue)
            Text(column.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            CountPill("Cards", cards.size, TextSecondary)
            IconButton(onClick = onAddCard) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp), tint = Blue) }
        }
        if (cards.isEmpty()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceLight).padding(16.dp), contentAlignment = Alignment.Center) {
                Text("No cards", fontSize = 12.sp, color = TextTertiary)
            }
        } else {
            cards.forEach { card ->
                ProjectCardRow(card, canMove = allColumns.size > 1, onMove = { onMoveCard(card) }, onDelete = { onDeleteCard(card) })
            }
        }
    }
}

@Composable
private fun ProjectCardRow(card: GHProjectCard, canMove: Boolean, onMove: () -> Unit, onDelete: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(SurfaceLight).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (card.archived) TextTertiary else Blue))
        Column(Modifier.weight(1f)) {
            Text(card.note.ifBlank { card.contentUrl.ifBlank { "Linked card" } }, fontSize = 13.sp, color = TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(card.updatedAt.take(10), fontSize = 10.sp, color = TextTertiary)
        }
        if (canMove) IconButton(onClick = onMove) { Icon(Icons.Rounded.ArrowForward, null, Modifier.size(18.dp), tint = Blue) }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Close, null, Modifier.size(18.dp), tint = Color(0xFFFF3B30)) }
    }
}

@Composable
private fun ProjectEditorDialog(
    title: String,
    initialName: String,
    initialBody: String,
    initialState: String,
    showState: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var body by remember(initialBody) { mutableStateOf(initialBody) }
    var state by remember(initialState) { mutableStateOf(initialState.ifBlank { "open" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Description") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth())
                if (showState) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProjectChip("Open", state == "open") { state = "open" }
                        ProjectChip("Closed", state == "closed") { state = "closed" }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, body, state) }) { Text(confirmLabel, color = Blue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    minLines: Int = 1,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = { Text(title, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                minLines = minLines,
                maxLines = if (minLines > 1) 8 else 1,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value) }) { Text(confirmLabel, color = Blue) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
private fun MoveCardDialog(
    card: GHProjectCard,
    fromColumn: GHProjectColumn,
    columns: List<GHProjectColumn>,
    onDismiss: () -> Unit,
    onMove: (GHProjectColumn) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = { Text("Move Card", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(card.note.ifBlank { "Linked card" }, fontSize = 13.sp, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                columns.filter { it.id != fromColumn.id }.forEach { column ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(SurfaceLight).clickable { onMove(column) }.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(16.dp), tint = Blue)
                        Text(column.name, fontSize = 13.sp, color = TextPrimary)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }
    )
}

@Composable
private fun ProjectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(if (selected) Blue.copy(0.15f) else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 12.sp, color = if (selected) Blue else TextPrimary, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CountPill(label: String, count: Int, color: Color, showCount: Boolean = true) {
    Row(
        Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(0.10f)).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
        if (showCount) Text("$count", fontSize = 11.sp, color = color)
    }
}

@Composable
private fun EmptyProjectsCard(message: String) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceWhite).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 14.sp, color = TextTertiary)
    }
}
