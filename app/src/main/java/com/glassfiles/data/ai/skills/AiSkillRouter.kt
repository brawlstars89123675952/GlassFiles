package com.glassfiles.data.ai.skills

import android.content.Context
import java.util.Locale

class AiSkillRouter(
    private val store: AiSkillStore = AiSkillStore,
) {
    fun match(context: Context, userMessage: String, appContext: AppAgentContext): AiSkillMatch? {
        if (!AiSkillPrefs.getEnableSkills(context) || !AiSkillPrefs.getAutoSuggest(context)) return null
        val lower = userMessage.lowercase(Locale.US)
        val candidates = store.listSkills(context).filter { it.enabled }
        val scored = candidates.mapNotNull { skill ->
            var best = 0f
            var reason = ""
            skill.triggers.forEach { trigger ->
                val t = trigger.lowercase(Locale.US).trim()
                if (t.isBlank()) return@forEach
                when {
                    lower == t -> if (1f > best) {
                        best = 1f
                        reason = "exact trigger: $trigger"
                    }
                    lower.contains(t) -> if (0.8f > best) {
                        best = 0.8f
                        reason = "contains trigger: $trigger"
                    }
                }
            }
            if (best < 0.5f && skill.category.isNotBlank() && lower.contains(skill.category.lowercase(Locale.US))) {
                best = 0.5f
                reason = "category keyword: ${skill.category}"
            }
            if (appContext.chatOnly && skill.tools.any { it.startsWith("github_") }) {
                best -= 0.1f
            }
            if (best >= 0.55f) AiSkillMatch(skill, best, reason) else null
        }
        return scored.maxByOrNull { it.confidence }
    }
}
