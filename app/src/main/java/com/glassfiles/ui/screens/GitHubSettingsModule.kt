package com.glassfiles.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.glassfiles.data.Strings
import com.glassfiles.data.github.GHNotification
import com.glassfiles.data.github.GHOrg
import com.glassfiles.data.github.GHRepo
import com.glassfiles.data.github.GHUser
import com.glassfiles.data.github.GHUserProfile
import com.glassfiles.data.github.GitHubManager
import com.glassfiles.ui.theme.Blue
import com.glassfiles.ui.theme.SeparatorColor
import com.glassfiles.ui.theme.SurfaceLight
import com.glassfiles.ui.theme.SurfaceWhite
import com.glassfiles.ui.theme.TextPrimary
import com.glassfiles.ui.theme.TextSecondary
import com.glassfiles.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private enum class SettingsSection(val title: String, val subtitle: String) {
    PROFILE("Profile", "Name, bio, company, location"),
    EMAILS("Emails", "Primary email and visibility"),
    NOTIFICATIONS("Notifications", "Unread filter and mark read"),
    KEYS("Keys", "SSH, SSH signing and GPG"),
    SOCIAL("Social accounts", "Linked social profiles"),
    PEOPLE("People", "Followers and following"),
    BLOCKED("Blocked users", "Block and unblock users"),
    INTERACTION("Interaction limits", "Temporary public interaction limits"),
    ORGANIZATIONS("Organizations", "Your organizations"),
    REPOSITORIES("Repositories", "Starred repositories"),
    DEVELOPER("Developer", "Token and cache")
}
private enum class KeyMode { SSH, SSH_SIGNING, GPG }

private data class EmailEntry(val email: String, val primary: Boolean, val verified: Boolean, val visibility: String)
private data class KeyEntry(val id: Long, val title: String, val key: String, val createdAt: String, val kind: String)
private data class SocialAccountEntry(val provider: String, val url: String)
private data class PersonEntry(val login: String, val avatarUrl: String)
private data class BlockedEntry(val login: String, val avatarUrl: String)
private data class InteractionLimitEntry(val limit: String, val expiry: String?)

@Composable
internal fun GitHubSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onClose: (() -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<GHUser?>(GitHubManager.getCachedUser(context)) }
    var currentSection by remember { mutableStateOf<SettingsSection?>(null) }
    var loading by remember { mutableStateOf(false) }

    var profile by remember { mutableStateOf<GHUserProfile?>(null) }
    var profileName by remember { mutableStateOf("") }
    var profileBio by remember { mutableStateOf("") }
    var profileCompany by remember { mutableStateOf("") }
    var profileLocation by remember { mutableStateOf("") }
    var profileBlog by remember { mutableStateOf("") }

    var emails by remember { mutableStateOf<List<EmailEntry>>(emptyList()) }
    var newEmail by remember { mutableStateOf("") }
    var emailVisibility by remember { mutableStateOf("private") }

    var notifications by remember { mutableStateOf<List<GHNotification>>(emptyList()) }
    var notificationsUnreadOnly by remember { mutableStateOf(true) }

    var keyMode by remember { mutableStateOf(KeyMode.SSH) }
    var sshKeys by remember { mutableStateOf<List<KeyEntry>>(emptyList()) }
    var sshSigningKeys by remember { mutableStateOf<List<KeyEntry>>(emptyList()) }
    var gpgKeys by remember { mutableStateOf<List<KeyEntry>>(emptyList()) }
    var keyTitle by remember { mutableStateOf("") }
    var keyValue by remember { mutableStateOf("") }

    var socialAccounts by remember { mutableStateOf<List<SocialAccountEntry>>(emptyList()) }
    var newSocialUrl by remember { mutableStateOf("") }

    var followers by remember { mutableStateOf<List<PersonEntry>>(emptyList()) }
    var following by remember { mutableStateOf<List<PersonEntry>>(emptyList()) }

    var blockedUsers by remember { mutableStateOf<List<BlockedEntry>>(emptyList()) }
    var blockUsername by remember { mutableStateOf("") }

    var interactionLimit by remember { mutableStateOf<InteractionLimitEntry?>(null) }
    var organizations by remember { mutableStateOf<List<GHOrg>>(emptyList()) }
    var starredRepos by remember { mutableStateOf<List<GHRepo>>(emptyList()) }
    var rateLimitSummary by remember { mutableStateOf("Unavailable") }
    var showChangeToken by remember { mutableStateOf(false) }
    var newToken by remember { mutableStateOf("") }
    val actionLog = remember { mutableStateListOf<String>() }

    fun addLog(line: String) {
        actionLog.add(0, line)
        while (actionLog.size > 30) actionLog.removeLast()
    }

    suspend fun reloadUserHeader() {
        user = GitHubManager.getUser(context) ?: GitHubManager.getCachedUser(context)
    }

    suspend fun refreshSection(section: SettingsSection?) {
        loading = true
        val token = GitHubManager.getToken(context)
        when (section) {
            null -> reloadUserHeader()
            SettingsSection.PROFILE -> {
                reloadUserHeader()
                val login = user?.login.orEmpty()
                profile = if (login.isBlank()) null else GitHubManager.getUserProfile(context, login)
                profileName = profile?.name ?: user?.name.orEmpty()
                profileBio = profile?.bio.orEmpty()
                profileCompany = profile?.company.orEmpty()
                profileLocation = profile?.location.orEmpty()
                profileBlog = profile?.blog.orEmpty()
            }
            SettingsSection.EMAILS -> {
                emails = ghGetEmails(token)
                emailVisibility = emails.firstOrNull { it.primary }?.visibility?.ifBlank { "private" } ?: "private"
            }
            SettingsSection.NOTIFICATIONS -> notifications = GitHubManager.getNotifications(context, all = !notificationsUnreadOnly)
            SettingsSection.KEYS -> {
                sshKeys = ghGetKeys(token, KeyMode.SSH)
                sshSigningKeys = ghGetKeys(token, KeyMode.SSH_SIGNING)
                gpgKeys = ghGetKeys(token, KeyMode.GPG)
            }
            SettingsSection.SOCIAL -> socialAccounts = ghGetSocialAccounts(token)
            SettingsSection.PEOPLE -> {
                followers = ghGetFollowers(token)
                following = ghGetFollowing(token)
            }
            SettingsSection.BLOCKED -> blockedUsers = ghGetBlockedUsers(token)
            SettingsSection.INTERACTION -> interactionLimit = ghGetInteractionLimit(token)
            SettingsSection.ORGANIZATIONS -> organizations = GitHubManager.getOrganizations(context)
            SettingsSection.REPOSITORIES -> starredRepos = GitHubManager.getStarredRepos(context)
            SettingsSection.DEVELOPER -> rateLimitSummary = ghGetRateLimitSummary(token)
        }
        loading = false
    }

    LaunchedEffect(currentSection) { refreshSection(currentSection) }

    Column(Modifier.fillMaxSize().background(SurfaceLight)) {
        SettingsTopBar(
            title = currentSection?.title ?: "GitHub Settings",
            subtitle = currentSection?.let { user?.name?.takeIf { n -> n.isNotBlank() } ?: user?.login },
            onBack = { if (currentSection == null) onBack() else currentSection = null },
            onClose = onClose,
            loading = loading,
            onRefresh = { scope.launch { refreshSection(currentSection) } }
        )

        if (currentSection == null) {
            HomeSettingsMenu(user = user, onOpen = { currentSection = it })
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { HeaderCard(user) }
                item {
                    when (currentSection) {
                        SettingsSection.PROFILE -> SectionCard("Profile") {
                            CompactField("Name", profileName) { profileName = it }
                            CompactField("Bio", profileBio, singleLine = false, minLines = 3) { profileBio = it }
                            CompactField("Company", profileCompany) { profileCompany = it }
                            CompactField("Location", profileLocation) { profileLocation = it }
                            CompactField("Blog", profileBlog) { profileBlog = it }
                            ActionRow(Icons.Rounded.Check, "Save profile") {
                                scope.launch {
                                    val ok = ghUpdateProfile(GitHubManager.getToken(context), profileName, profileBio, profileCompany, profileLocation, profileBlog)
                                    addLog("Profile updated: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.PROFILE)
                                }
                            }
                        }
                        SettingsSection.EMAILS -> SectionCard("Emails") {
                            VisibilityChooser(emailVisibility) { emailVisibility = it }
                            ActionRow(Icons.Rounded.Check, "Apply visibility") {
                                scope.launch {
                                    val ok = ghSetEmailVisibility(GitHubManager.getToken(context), emailVisibility)
                                    addLog("Email visibility: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.EMAILS)
                                }
                            }
                            CompactField("Add email", newEmail) { newEmail = it }
                            ActionRow(Icons.Rounded.Add, "Add email") {
                                scope.launch {
                                    val ok = ghAddEmail(GitHubManager.getToken(context), newEmail)
                                    addLog("Add email: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    if (ok) newEmail = ""
                                    refreshSection(SettingsSection.EMAILS)
                                }
                            }
                        }
                        SettingsSection.NOTIFICATIONS -> SectionCard("Notifications") {
                            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Unread only", color = TextPrimary, fontSize = 14.sp)
                                Switch(
                                    checked = notificationsUnreadOnly,
                                    onCheckedChange = {
                                        notificationsUnreadOnly = it
                                        scope.launch { refreshSection(SettingsSection.NOTIFICATIONS) }
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Blue)
                                )
                            }
                            ActionRow(Icons.Rounded.Check, "Mark all read") {
                                scope.launch {
                                    val ok = GitHubManager.markAllNotificationsRead(context)
                                    addLog("Mark all read: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.NOTIFICATIONS)
                                }
                            }
                        }
                        SettingsSection.KEYS -> SectionCard("Keys") {
                            KeyModeRow(keyMode) { keyMode = it }
                            CompactField(if (keyMode == KeyMode.GPG) "Name" else "Title", keyTitle) { keyTitle = it }
                            CompactField(if (keyMode == KeyMode.GPG) "ASCII-armored key" else "Public key", keyValue, singleLine = false, minLines = 4) { keyValue = it }
                            ActionRow(Icons.Rounded.Add, "Add key") {
                                scope.launch {
                                    val ok = ghAddKey(GitHubManager.getToken(context), keyMode, keyTitle, keyValue)
                                    addLog("Add key: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    if (ok) { keyTitle = ""; keyValue = "" }
                                    refreshSection(SettingsSection.KEYS)
                                }
                            }
                        }
                        SettingsSection.SOCIAL -> SectionCard("Social accounts") {
                            CompactField("Add social URL", newSocialUrl) { newSocialUrl = it }
                            ActionRow(Icons.Rounded.Add, "Add social account") {
                                scope.launch {
                                    val ok = ghAddSocialAccount(GitHubManager.getToken(context), newSocialUrl)
                                    addLog("Add social account: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    if (ok) newSocialUrl = ""
                                    refreshSection(SettingsSection.SOCIAL)
                                }
                            }
                        }
                        SettingsSection.PEOPLE -> SectionCard("People") {
                            Text("Followers", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (followers.isEmpty()) Text("No followers", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            followers.forEach { person ->
                                CompactPersonRow(person.login, person.avatarUrl, "Follow") {
                                    scope.launch {
                                        val ok = GitHubManager.followUser(context, person.login)
                                        addLog("Follow ${person.login}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Following", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            if (following.isEmpty()) Text("Not following anyone", color = TextTertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                            following.forEach { person ->
                                CompactPersonRow(person.login, person.avatarUrl, "Unfollow") {
                                    scope.launch {
                                        val ok = GitHubManager.unfollowUser(context, person.login)
                                        addLog("Unfollow ${person.login}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.PEOPLE)
                                    }
                                }
                            }
                        }
                        SettingsSection.BLOCKED -> SectionCard("Blocked users") {
                            CompactField("Block username", blockUsername) { blockUsername = it }
                            ActionRow(Icons.Rounded.Block, "Block user") {
                                scope.launch {
                                    val ok = ghBlockUser(GitHubManager.getToken(context), blockUsername)
                                    addLog("Block ${blockUsername}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    if (ok) blockUsername = ""
                                    refreshSection(SettingsSection.BLOCKED)
                                }
                            }
                        }
                        SettingsSection.INTERACTION -> SectionCard("Interaction limits") {
                            Text(interactionLimit?.let { "Current: ${it.limit}${it.expiry?.let { exp -> " • $exp" } ?: ""}" } ?: "No active interaction limit", color = TextSecondary, fontSize = 12.sp)
                            ActionRow(Icons.Rounded.Warning, "Existing users for 24h") {
                                scope.launch {
                                    val ok = ghSetInteractionLimit(GitHubManager.getToken(context), "existing_users", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Warning, "Contributors only for 24h") {
                                scope.launch {
                                    val ok = ghSetInteractionLimit(GitHubManager.getToken(context), "contributors_only", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Warning, "Collaborators only for 24h") {
                                scope.launch {
                                    val ok = ghSetInteractionLimit(GitHubManager.getToken(context), "collaborators_only", "one_day")
                                    addLog("Set interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                            ActionRow(Icons.Rounded.Delete, "Remove interaction limit", tint = Color(0xFFFF3B30)) {
                                scope.launch {
                                    val ok = ghRemoveInteractionLimit(GitHubManager.getToken(context))
                                    addLog("Remove interaction limit: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.INTERACTION)
                                }
                            }
                        }
                        SettingsSection.ORGANIZATIONS -> SectionCard("Organizations") {
                            if (organizations.isEmpty()) Text("No organizations", color = TextTertiary, fontSize = 12.sp)
                            organizations.forEach { org -> CompactOrgRow(org) }
                        }
                        SettingsSection.REPOSITORIES -> SectionCard("Repositories") {
                            if (starredRepos.isEmpty()) Text("No starred repositories", color = TextTertiary, fontSize = 12.sp)
                            starredRepos.forEach { repo ->
                                CompactRepoRow(repo) {
                                    scope.launch {
                                        val ok = GitHubManager.unstarRepo(context, repo.owner, repo.name)
                                        addLog("Unstar ${repo.fullName}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.REPOSITORIES)
                                    }
                                }
                            }
                        }
                        SettingsSection.DEVELOPER -> SectionCard("Developer") {
                            InfoLine("Token", maskToken(GitHubManager.getToken(context)))
                            InfoLine("Rate limit", rateLimitSummary)
                            ActionRow(Icons.Rounded.Key, "Change token") { showChangeToken = true }
                            ActionRow(Icons.Rounded.Logout, "Sign out", tint = Color(0xFFFF3B30)) {
                                GitHubManager.logout(context)
                                onLogout()
                            }
                            if (actionLog.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Recent actions", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                actionLog.forEach { line -> Text(line, color = TextTertiary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                            }
                        }
                        null -> Unit
                    }
                }

                when (currentSection) {
                    SettingsSection.EMAILS -> items(emails) { email ->
                        CompactCard {
                            EmailRow(email) {
                                scope.launch {
                                    val ok = ghDeleteEmail(GitHubManager.getToken(context), email.email)
                                    addLog("Delete email ${email.email}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.EMAILS)
                                }
                            }
                        }
                    }
                    SettingsSection.NOTIFICATIONS -> items(notifications) { item ->
                        CompactCard {
                            NotificationRow(item) {
                                scope.launch {
                                    val ok = GitHubManager.markNotificationRead(context, item.id)
                                    addLog("Mark thread ${item.id}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.NOTIFICATIONS)
                                }
                            }
                        }
                    }
                    SettingsSection.KEYS -> {
                        val currentKeys = when (keyMode) {
                            KeyMode.SSH -> sshKeys
                            KeyMode.SSH_SIGNING -> sshSigningKeys
                            KeyMode.GPG -> gpgKeys
                        }
                        items(currentKeys) { key ->
                            CompactCard {
                                KeyRow(key) {
                                    scope.launch {
                                        val ok = ghDeleteKey(GitHubManager.getToken(context), keyMode, key.id)
                                        addLog("Delete key ${key.id}: $ok")
                                        Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                        refreshSection(SettingsSection.KEYS)
                                    }
                                }
                            }
                        }
                    }
                    SettingsSection.SOCIAL -> items(socialAccounts) { acc ->
                        CompactCard {
                            SocialRow(acc) {
                                scope.launch {
                                    val ok = ghDeleteSocialAccount(GitHubManager.getToken(context), acc.url)
                                    addLog("Delete social ${acc.url}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.SOCIAL)
                                }
                            }
                        }
                    }
                    SettingsSection.BLOCKED -> items(blockedUsers) { blocked ->
                        CompactCard {
                            BlockedRow(blocked) {
                                scope.launch {
                                    val ok = ghUnblockUser(GitHubManager.getToken(context), blocked.login)
                                    addLog("Unblock ${blocked.login}: $ok")
                                    Toast.makeText(context, if (ok) Strings.done else Strings.error, Toast.LENGTH_SHORT).show()
                                    refreshSection(SettingsSection.BLOCKED)
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    if (showChangeToken) {
        AlertDialog(
            onDismissRequest = { showChangeToken = false },
            containerColor = SurfaceWhite,
            title = { Text("Change token", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { CompactField("Personal access token", newToken, singleLine = true, password = true) { newToken = it } },
            confirmButton = {
                TextButton(onClick = {
                    GitHubManager.saveToken(context, newToken.trim())
                    addLog("Token updated")
                    newToken = ""
                    showChangeToken = false
                    scope.launch {
                        reloadUserHeader()
                        refreshSection(SettingsSection.DEVELOPER)
                    }
                }) { Text(Strings.done, color = Blue) }
            },
            dismissButton = { TextButton(onClick = { showChangeToken = false }) { Text(Strings.cancel, color = TextSecondary) } }
        )
    }
}

private suspend fun ghRequest(token: String, path: String, method: String = "GET", body: String? = null): Pair<Int, String> = withContext(Dispatchers.IO) {
    try {
        val conn = (URL("https://api.github.com$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                outputStream.use { it.write(body.toByteArray()) }
            }
        }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        code to text
    } catch (_: Exception) {
        -1 to ""
    }
}

private suspend fun ghGetEmails(token: String): List<EmailEntry> { val (code, body) = ghRequest(token, "/user/emails"); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(EmailEntry(o.optString("email"), o.optBoolean("primary"), o.optBoolean("verified"), o.optString("visibility"))) } } }
private suspend fun ghAddEmail(token: String, email: String): Boolean { val arr = JSONArray().put(email).toString(); val (code, _) = ghRequest(token, "/user/emails", "POST", arr); return code in 200..299 }
private suspend fun ghDeleteEmail(token: String, email: String): Boolean { val arr = JSONArray().put(email).toString(); val (code, _) = ghRequest(token, "/user/emails", "DELETE", arr); return code in 200..299 }
private suspend fun ghSetEmailVisibility(token: String, visibility: String): Boolean { val body = JSONObject().put("visibility", visibility).toString(); val (code, _) = ghRequest(token, "/user/email/visibility", "PATCH", body); return code in 200..299 }
private suspend fun ghUpdateProfile(token: String, name: String, bio: String, company: String, location: String, blog: String): Boolean { val body = JSONObject().apply { put("name", name); put("bio", bio); put("company", company); put("location", location); put("blog", blog) }.toString(); val (code, _) = ghRequest(token, "/user", "PATCH", body); return code in 200..299 }
private suspend fun ghGetKeys(token: String, mode: KeyMode): List<KeyEntry> { val path = when (mode) { KeyMode.SSH -> "/user/keys"; KeyMode.SSH_SIGNING -> "/user/ssh_signing_keys"; KeyMode.GPG -> "/user/gpg_keys" }; val (code, body) = ghRequest(token, path); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(KeyEntry(o.optLong("id"), if (mode == KeyMode.GPG) o.optString("name") else o.optString("title"), o.optString(if (mode == KeyMode.GPG) "raw_key" else "key"), o.optString("created_at"), when (mode) { KeyMode.SSH -> "ssh"; KeyMode.SSH_SIGNING -> "ssh-signing"; KeyMode.GPG -> "gpg" })) } } }
private suspend fun ghAddKey(token: String, mode: KeyMode, title: String, key: String): Boolean { val path = when (mode) { KeyMode.SSH -> "/user/keys"; KeyMode.SSH_SIGNING -> "/user/ssh_signing_keys"; KeyMode.GPG -> "/user/gpg_keys" }; val body = when (mode) { KeyMode.GPG -> JSONObject().put("armored_public_key", key).toString(); else -> JSONObject().put("title", title).put("key", key).toString() }; val (code, _) = ghRequest(token, path, "POST", body); return code in 200..299 }
private suspend fun ghDeleteKey(token: String, mode: KeyMode, id: Long): Boolean { val path = when (mode) { KeyMode.SSH -> "/user/keys/$id"; KeyMode.SSH_SIGNING -> "/user/ssh_signing_keys/$id"; KeyMode.GPG -> "/user/gpg_keys/$id" }; val (code, _) = ghRequest(token, path, "DELETE"); return code in 200..299 }
private suspend fun ghGetSocialAccounts(token: String): List<SocialAccountEntry> { val (code, body) = ghRequest(token, "/user/social_accounts"); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(SocialAccountEntry(o.optString("provider"), o.optString("url"))) } } }
private suspend fun ghAddSocialAccount(token: String, url: String): Boolean { val body = JSONArray().put(url).toString(); val (code, _) = ghRequest(token, "/user/social_accounts", "POST", body); return code in 200..299 }
private suspend fun ghDeleteSocialAccount(token: String, url: String): Boolean { val body = JSONArray().put(url).toString(); val (code, _) = ghRequest(token, "/user/social_accounts", "DELETE", body); return code in 200..299 }
private suspend fun ghGetFollowers(token: String): List<PersonEntry> { val (code, body) = ghRequest(token, "/user/followers?per_page=100"); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(PersonEntry(o.optString("login"), o.optString("avatar_url"))) } } }
private suspend fun ghGetFollowing(token: String): List<PersonEntry> { val (code, body) = ghRequest(token, "/user/following?per_page=100"); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(PersonEntry(o.optString("login"), o.optString("avatar_url"))) } } }
private suspend fun ghGetBlockedUsers(token: String): List<BlockedEntry> { val (code, body) = ghRequest(token, "/user/blocks?per_page=100"); if (code !in 200..299 || body.isBlank()) return emptyList(); val arr = JSONArray(body); return buildList { for (i in 0 until arr.length()) { val o = arr.getJSONObject(i); add(BlockedEntry(o.optString("login"), o.optString("avatar_url"))) } } }
private suspend fun ghBlockUser(token: String, username: String): Boolean { val (code, _) = ghRequest(token, "/user/blocks/$username", "PUT", ""); return code in 200..299 }
private suspend fun ghUnblockUser(token: String, username: String): Boolean { val (code, _) = ghRequest(token, "/user/blocks/$username", "DELETE"); return code in 200..299 }
private suspend fun ghGetInteractionLimit(token: String): InteractionLimitEntry? { val (code, body) = ghRequest(token, "/user/interaction-limits"); if (code !in 200..299 || body.isBlank()) return null; val o = JSONObject(body); return InteractionLimitEntry(o.optString("limit"), o.optString("expires_at").ifBlank { null }) }
private suspend fun ghSetInteractionLimit(token: String, limit: String, expiry: String): Boolean { val body = JSONObject().put("limit", limit).put("expiry", expiry).toString(); val (code, _) = ghRequest(token, "/user/interaction-limits", "PUT", body); return code in 200..299 }
private suspend fun ghRemoveInteractionLimit(token: String): Boolean { val (code, _) = ghRequest(token, "/user/interaction-limits", "DELETE"); return code in 200..299 }
private suspend fun ghGetRateLimitSummary(token: String): String { val (code, body) = ghRequest(token, "/rate_limit"); if (code !in 200..299 || body.isBlank()) return "Unavailable"; return try { val core = JSONObject(body).getJSONObject("resources").getJSONObject("core"); "${core.optInt("remaining")} / ${core.optInt("limit")}" } catch (_: Exception) { "Unavailable" } }