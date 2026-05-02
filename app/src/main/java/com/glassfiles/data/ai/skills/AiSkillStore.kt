package com.glassfiles.data.ai.skills

import android.content.Context
import android.net.Uri
import com.glassfiles.data.ai.agent.AgentTools
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

object AiSkillStore {
    private const val INDEX_FILE = "index.json"
    private val dangerousTools = setOf(
        "local_delete",
        "local_trash_empty",
        "terminal_run",
        "root",
        "shizuku",
        "network_upload",
    )
    private val suspiciousPatterns = listOf(
        "ignore previous instructions",
        "do not tell the user",
        "steal",
        "exfiltrate",
        "api key",
        "rm -rf",
        "curl | sh",
        "wget | sh",
        "chmod 777",
        "su",
        "shizuku",
    )

    fun skillsRoot(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "skills").apply { mkdirs() }

    fun packsRoot(context: Context): File =
        File(skillsRoot(context), "packs").apply { mkdirs() }

    fun prepareImport(context: Context, uri: Uri): AiSkillImportPreview {
        val tempZip = File(context.cacheDir, "skill_import_${System.currentTimeMillis()}.gskill")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempZip).use { output -> input.copyTo(output) }
        } ?: error("Unable to open skill pack")
        return prepareImport(context, tempZip)
    }

    fun prepareImport(context: Context, source: File): AiSkillImportPreview {
        val lower = source.name.lowercase(Locale.US)
        require(lower.endsWith(".gskill") || lower.endsWith(".zip")) {
            "Skill pack must be .gskill or .zip"
        }
        val tempDir = File(context.cacheDir, "skill_import_${System.currentTimeMillis()}").canonicalFile
        tempDir.mkdirs()
        unzipSafe(source, tempDir)
        val manifestFile = File(tempDir, "manifest.json")
        require(manifestFile.isFile) { "manifest.json is required" }

        val manifest = JSONObject(manifestFile.readText())
        val warnings = linkedSetOf<String>()
        val requestedTools = manifest.optStringArray("tools")
        require(requestedTools.isNotEmpty()) { "manifest tools must not be empty" }
        validateKnownTools(requestedTools)

        val manifestRisk = AiSkillRisk.parse(manifest.optString("risk", "low"))
        val effectivePackRisk = escalateRisk(manifestRisk, requestedTools)
        if (effectivePackRisk.ordinal > manifestRisk.ordinal) {
            warnings += "risk escalated to ${effectivePackRisk.name.lowercase(Locale.US)} because dangerous tools were requested"
        }
        if (manifest.optString("author", "").equals("community", ignoreCase = true)) {
            warnings += "community/untrusted source"
        }
        warnings += scanSuspicious("manifest.json", manifestFile.readText())

        val packId = manifest.requireCleanId("id")
        val pack = AiSkillPack(
            id = packId,
            name = manifest.optString("name", packId).ifBlank { packId },
            version = manifest.optString("version").ifBlank { error("manifest version is required") },
            author = manifest.optString("author").takeIf { it.isNotBlank() },
            description = manifest.optString("description").takeIf { it.isNotBlank() },
            source = manifest.optString("source").takeIf { it.isNotBlank() },
            risk = effectivePackRisk,
            permissions = manifest.optStringArray("permissions"),
            tools = requestedTools,
            minAppVersion = manifest.optInt("minAppVersion").takeIf { it > 0 },
            installedAt = System.currentTimeMillis(),
            enabled = true,
            trusted = false,
        )

        val skillsDir = File(tempDir, "skills").canonicalFile
        require(skillsDir.isDirectory) { "skills/ directory is required" }
        val skills = skillsDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".skill.md", ignoreCase = true) }
            .map { file ->
                warnings += scanSuspicious(file.name, file.readText())
                parseSkillFile(file, pack)
            }
            .toList()
        require(skills.isNotEmpty()) { "At least one .skill.md file is required" }
        skills.forEach { skill ->
            require(skill.triggers.isNotEmpty()) { "${skill.id}: triggers are required" }
            require(skill.instructions.isNotBlank()) { "${skill.id}: instructions are required" }
            validateKnownTools(skill.tools)
            val missing = skill.tools.filterNot { it in pack.tools }
            require(missing.isEmpty()) { "${skill.id}: tools not declared in manifest: ${missing.joinToString()}" }
        }
        return AiSkillImportPreview(tempDir.absolutePath, pack, skills, warnings.toList())
    }

    fun commitImport(context: Context, preview: AiSkillImportPreview): AiSkillPack {
        val src = File(preview.tempDirPath).canonicalFile
        require(src.isDirectory) { "Import temp directory is missing" }
        val dest = File(packsRoot(context), preview.pack.id).canonicalFile
        require(dest.path.startsWith(packsRoot(context).canonicalPath + File.separator)) {
            "Invalid pack id"
        }
        if (dest.exists()) dest.deleteRecursively()
        src.copyRecursively(dest, overwrite = true)
        updateIndex(context) { index ->
            val packs = index.optJSONObject("packs") ?: JSONObject().also { index.put("packs", it) }
            packs.put(
                preview.pack.id,
                JSONObject()
                    .put("enabled", true)
                    .put("trusted", false)
                    .put("installedAt", preview.pack.installedAt),
            )
            val skills = index.optJSONObject("skills") ?: JSONObject().also { index.put("skills", it) }
            preview.skills.forEach { skill ->
                skills.put("${preview.pack.id}/${skill.id}", JSONObject().put("enabled", true))
            }
        }
        return preview.pack
    }

    fun listPacks(context: Context): List<AiSkillPack> {
        val index = readIndex(context)
        val packsMeta = index.optJSONObject("packs") ?: JSONObject()
        return packsRoot(context).listFiles().orEmpty()
            .filter { it.isDirectory }
            .mapNotNull { dir ->
                runCatching {
                    val manifest = JSONObject(File(dir, "manifest.json").readText())
                    val id = manifest.requireCleanId("id")
                    val meta = packsMeta.optJSONObject(id) ?: JSONObject()
                    val tools = manifest.optStringArray("tools")
                    AiSkillPack(
                        id = id,
                        name = manifest.optString("name", id).ifBlank { id },
                        version = manifest.optString("version", ""),
                        author = manifest.optString("author").takeIf { it.isNotBlank() },
                        description = manifest.optString("description").takeIf { it.isNotBlank() },
                        source = manifest.optString("source").takeIf { it.isNotBlank() },
                        risk = escalateRisk(AiSkillRisk.parse(manifest.optString("risk", "low")), tools),
                        permissions = manifest.optStringArray("permissions"),
                        tools = tools,
                        minAppVersion = manifest.optInt("minAppVersion").takeIf { it > 0 },
                        installedAt = meta.optLong("installedAt", dir.lastModified()),
                        enabled = meta.optBoolean("enabled", true),
                        trusted = meta.optBoolean("trusted", false),
                    )
                }.getOrNull()
            }
            .sortedBy { it.name.lowercase(Locale.US) }
    }

    fun listSkills(context: Context): List<AiSkill> {
        val index = readIndex(context)
        val skillMeta = index.optJSONObject("skills") ?: JSONObject()
        return listPacks(context).flatMap { pack ->
            val dir = File(packsRoot(context), pack.id)
            File(dir, "skills").walkTopDown()
                .filter { it.isFile && it.name.endsWith(".skill.md", ignoreCase = true) }
                .mapNotNull { file ->
                    runCatching {
                        val parsed = parseSkillFile(file, pack)
                        val key = "${pack.id}/${parsed.id}"
                        parsed.copy(enabled = pack.enabled && skillMeta.optJSONObject(key)?.optBoolean("enabled", true) != false)
                    }.getOrNull()
                }
                .toList()
        }
    }

    fun readSkill(context: Context, skillId: String): AiSkill? =
        listSkills(context).firstOrNull { it.id == skillId || "${it.packId}/${it.id}" == skillId }

    fun setSkillEnabled(context: Context, packId: String, skillId: String, enabled: Boolean) {
        updateIndex(context) { index ->
            val skills = index.optJSONObject("skills") ?: JSONObject().also { index.put("skills", it) }
            skills.put("$packId/$skillId", JSONObject().put("enabled", enabled))
        }
    }

    fun setPackEnabled(context: Context, packId: String, enabled: Boolean) {
        updateIndex(context) { index ->
            val packs = index.optJSONObject("packs") ?: JSONObject().also { index.put("packs", it) }
            val meta = packs.optJSONObject(packId) ?: JSONObject()
            packs.put(packId, meta.put("enabled", enabled))
        }
    }

    fun deletePack(context: Context, packId: String) {
        File(packsRoot(context), packId).deleteRecursively()
        updateIndex(context) { index ->
            index.optJSONObject("packs")?.remove(packId)
            val skills = index.optJSONObject("skills") ?: return@updateIndex
            val keys = skills.keys().asSequence().toList()
            keys.filter { it.startsWith("$packId/") }.forEach { skills.remove(it) }
        }
    }

    fun allowedToolsForSkill(context: Context, skill: AiSkill): Set<String> {
        val pack = listPacks(context).firstOrNull { it.id == skill.packId }
        val allowDangerous = AiSkillPrefs.getAllowUntrustedDangerousTools(context)
        return skill.tools
            .filter { AgentTools.byName(it) != null }
            .filter { tool -> pack?.trusted == true || allowDangerous || tool !in dangerousTools }
            .toSet()
    }

    fun promptFor(skill: AiSkill, allowedTools: Set<String>): String = buildString {
        appendLine("Active skill:")
        appendLine("Name: ${skill.name}")
        appendLine("Risk: ${skill.risk.name.lowercase(Locale.US)}")
        appendLine("Allowed tools:")
        allowedTools.sorted().forEach { appendLine("- $it") }
        appendLine()
        appendLine("Skill instructions:")
        appendLine(skill.instructions)
    }

    fun isDangerousTool(toolName: String): Boolean =
        toolName in dangerousTools || toolName.contains("delete", ignoreCase = true)

    private fun parseSkillFile(file: File, pack: AiSkillPack): AiSkill {
        val raw = file.readText()
        require(raw.startsWith("---")) { "${file.name}: YAML frontmatter is required" }
        val end = raw.indexOf("\n---", startIndex = 3)
        require(end > 0) { "${file.name}: closing frontmatter marker is required" }
        val yaml = raw.substring(3, end).trim()
        val instructions = raw.substring(end + 4).trim()
        val scalar = parseYamlScalars(yaml)
        val lists = parseYamlLists(yaml)
        val id = scalar["id"]?.cleanId() ?: error("${file.name}: id is required")
        return AiSkill(
            id = id,
            packId = pack.id,
            name = scalar["name"]?.takeIf { it.isNotBlank() } ?: id,
            description = scalar["description"]?.takeIf { it.isNotBlank() },
            category = scalar["category"]?.takeIf { it.isNotBlank() } ?: "general",
            risk = escalateRisk(AiSkillRisk.parse(scalar["risk"]), lists["tools"].orEmpty()),
            triggers = lists["triggers"].orEmpty(),
            tools = lists["tools"].orEmpty().ifEmpty { pack.tools },
            permissions = lists["permissions"].orEmpty().ifEmpty { pack.permissions },
            instructions = instructions,
            enabled = true,
        )
    }

    private fun unzipSafe(source: File, dest: File) {
        ZipInputStream(FileInputStream(source)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.replace('\\', '/')
                require(!name.startsWith("/") && !name.contains("..")) { "Blocked unsafe zip entry: $name" }
                val out = File(dest, name).canonicalFile
                require(out.path == dest.path || out.path.startsWith(dest.path + File.separator)) {
                    "Blocked Zip Slip entry: $name"
                }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun validateKnownTools(tools: List<String>) {
        val unknown = tools.filterNot { AgentTools.byName(it) != null }
        require(unknown.isEmpty()) { "Unknown requested tools: ${unknown.joinToString()}" }
    }

    private fun escalateRisk(risk: AiSkillRisk, tools: List<String>): AiSkillRisk =
        if (tools.any { it in dangerousTools || it.contains("delete", ignoreCase = true) }) {
            if (risk.ordinal < AiSkillRisk.HIGH.ordinal) AiSkillRisk.HIGH else risk
        } else risk

    private fun scanSuspicious(name: String, text: String): List<String> {
        val lower = text.lowercase(Locale.US)
        return suspiciousPatterns.filter { it in lower }.map { "$name contains suspicious phrase: $it" }
    }

    private fun readIndex(context: Context): JSONObject {
        val file = File(skillsRoot(context), INDEX_FILE)
        return runCatching { JSONObject(file.readText()) }.getOrDefault(JSONObject())
    }

    private fun updateIndex(context: Context, block: (JSONObject) -> Unit) {
        val index = readIndex(context)
        block(index)
        File(skillsRoot(context), INDEX_FILE).writeText(index.toString(2))
    }

    private fun JSONObject.optStringArray(key: String): List<String> {
        val arr = optJSONArray(key) ?: return emptyList()
        return (0 until arr.length())
            .mapNotNull { arr.optString(it).trim().takeIf { value -> value.isNotBlank() } }
            .distinct()
    }

    private fun JSONObject.requireCleanId(key: String): String =
        optString(key).cleanId() ?: error("$key is required")

    private fun String.cleanId(): String? {
        val clean = trim()
        if (clean.isBlank()) return null
        require(Regex("^[A-Za-z0-9_.-]+$").matches(clean)) { "Invalid id: $clean" }
        return clean
    }

    private fun parseYamlScalars(yaml: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        yaml.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("-") || !line.contains(":")) return@forEach
            val key = line.substringBefore(":").trim()
            val value = line.substringAfter(":").trim().trim('"', '\'')
            if (value.isNotBlank() && !value.startsWith("[") && !value.endsWith("|")) {
                out[key] = value
            }
        }
        return out
    }

    private fun parseYamlLists(yaml: String): Map<String, List<String>> {
        val out = linkedMapOf<String, MutableList<String>>()
        var current: String? = null
        yaml.lineSequence().forEach { raw ->
            val line = raw.trim()
            when {
                line.isBlank() -> {}
                line.contains(":") && !line.startsWith("-") -> {
                    val key = line.substringBefore(":").trim()
                    val tail = line.substringAfter(":").trim()
                    current = key
                    if (tail.startsWith("[") && tail.endsWith("]")) {
                        out.getOrPut(key) { mutableListOf() } += tail
                            .trim('[', ']')
                            .split(',')
                            .map { it.trim().trim('"', '\'') }
                            .filter { it.isNotBlank() }
                    }
                }
                line.startsWith("-") && current != null -> {
                    out.getOrPut(current!!) { mutableListOf() } += line.removePrefix("-").trim().trim('"', '\'')
                }
            }
        }
        return out
    }
}
