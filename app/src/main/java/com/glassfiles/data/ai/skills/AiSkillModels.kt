package com.glassfiles.data.ai.skills

data class AiSkillPack(
    val id: String,
    val name: String,
    val version: String,
    val author: String?,
    val description: String?,
    val whenToUse: String? = null,
    val source: String?,
    val risk: AiSkillRisk,
    val permissions: List<String>,
    val tools: List<String>,
    val minAppVersion: Int?,
    val installedAt: Long,
    val enabled: Boolean,
    val trusted: Boolean,
)

data class AiSkill(
    val id: String,
    val packId: String,
    val name: String,
    val description: String?,
    val whenToUse: String? = null,
    val category: String,
    val risk: AiSkillRisk,
    val triggers: List<String>,
    val tools: List<String>,
    val permissions: List<String>,
    val instructions: String,
    val enabled: Boolean,
)

enum class AiSkillRisk {
    READ_ONLY,
    LOW,
    MEDIUM,
    HIGH,
    DANGEROUS;

    companion object {
        fun parse(value: String?): AiSkillRisk =
            values().firstOrNull { it.name.equals(value.orEmpty().replace("-", "_"), ignoreCase = true) }
                ?: LOW
    }
}

data class AiSkillImportPreview(
    val tempDirPath: String,
    val pack: AiSkillPack,
    val skills: List<AiSkill>,
    val warnings: List<String>,
)

data class AppAgentContext(
    val repoFullName: String? = null,
    val chatOnly: Boolean = false,
    val currentScreen: String = "ai_agent",
    val currentPath: String? = null,
    val selectedFiles: List<String> = emptyList(),
    val clipboardFiles: List<String> = emptyList(),
    val activeBranch: String? = null,
    val activeProvider: String? = null,
    val language: String = "system",
    val localWorkspacePath: String? = null,
    val attachedFileName: String? = null,
    val attachedFilePath: String? = null,
    val attachedFileMime: String? = null,
    val attachedFileIsArchive: Boolean = false,
    val workspaceMode: Boolean = false,
) {
    fun toPromptBlock(): String = buildString {
        appendLine("## App context")
        appendLine("screen: $currentScreen")
        appendLine("mode: ${if (chatOnly) "chat-only" else "repository"}")
        appendLine("language: $language")
        appendLine("workspace mode: ${if (workspaceMode) "enabled" else "disabled"}")
        repoFullName?.takeIf { it.isNotBlank() }?.let { appendLine("repo: $it") }
        activeBranch?.takeIf { it.isNotBlank() }?.let { appendLine("branch: $it") }
        activeProvider?.takeIf { it.isNotBlank() }?.let { appendLine("provider: $it") }
        currentPath?.takeIf { it.isNotBlank() }?.let { appendLine("current path: $it") }
        localWorkspacePath?.takeIf { it.isNotBlank() }?.let {
            appendLine("local tool workspace: $it")
            appendLine("relative local_* and archive_* paths resolve inside this workspace")
        }
        if (selectedFiles.isNotEmpty()) {
            appendLine("selected files:")
            selectedFiles.take(20).forEach { appendLine("- $it") }
            if (selectedFiles.size > 20) appendLine("- ... ${selectedFiles.size - 20} more")
        }
        if (clipboardFiles.isNotEmpty()) {
            appendLine("clipboard files:")
            clipboardFiles.take(20).forEach { appendLine("- $it") }
            if (clipboardFiles.size > 20) appendLine("- ... ${clipboardFiles.size - 20} more")
        }
        attachedFileName?.takeIf { it.isNotBlank() }?.let { name ->
            appendLine("attached file:")
            appendLine("- name: $name")
            attachedFilePath?.takeIf { it.isNotBlank() }?.let { appendLine("- temp path: $it") }
            attachedFileMime?.takeIf { it.isNotBlank() }?.let { appendLine("- mime: $it") }
            appendLine("- archive: $attachedFileIsArchive")
        }
    }.trimEnd()
}

data class AiSkillMatch(
    val skill: AiSkill,
    val confidence: Float,
    val reason: String,
)
