package com.glassfiles.data.ai.skills

import android.content.Context

object AiSkillPrefs {
    private const val PREFS = "ai_skill_prefs"
    private const val KEY_ENABLE_SKILLS = "enable_skills"
    private const val KEY_AUTO_SUGGEST = "auto_suggest_matching_skill"
    private const val KEY_ALLOW_UNTRUSTED_DANGEROUS = "allow_untrusted_dangerous_tools"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getEnableSkills(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLE_SKILLS, true)

    fun setEnableSkills(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLE_SKILLS, enabled).apply()
    }

    fun getAutoSuggest(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_SUGGEST, true)

    fun setAutoSuggest(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SUGGEST, enabled).apply()
    }

    fun getAllowUntrustedDangerousTools(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ALLOW_UNTRUSTED_DANGEROUS, false)

    fun setAllowUntrustedDangerousTools(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ALLOW_UNTRUSTED_DANGEROUS, enabled).apply()
    }
}
