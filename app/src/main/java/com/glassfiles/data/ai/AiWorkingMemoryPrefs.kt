package com.glassfiles.data.ai

import android.content.Context

/**
 * Persisted toggle for the AI agent's working memory feature
 * (BUGS_FIX.md Section 3 — "Maintain working memory during tasks").
 *
 * When enabled the agent loop:
 *  • prepends the contents of working_memory.md to the system prompt,
 *  • injects "[system] Update working memory…" reminders after edits,
 *  • surfaces a `▸ N files` indicator in the topbar.
 *
 * When disabled none of the above runs and existing working_memory.md
 * files are left in place — the user can view / edit them through the
 * memory files dialog regardless.
 *
 * Default: ON. Working memory is a quality-of-life feature with no
 * extra-cost side effects (no extra API calls, no background work),
 * so opt-out is the right default per the BUGS_FIX.md acceptance
 * criteria.
 */
object AiWorkingMemoryPrefs {
    private const val PREFS = "ai_working_memory_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_REMINDERS = "reminders"
    private const val DEFAULT_ENABLED = true
    private const val DEFAULT_REMINDERS = true

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, DEFAULT_ENABLED)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /**
     * When ON the agent receives a `[system] Update working memory…`
     * reminder after every write_file / edit_file (subject to the
     * "more than 3 tool calls since last update OR new file" rule
     * from BUGS_FIX.md). When OFF the agent is still allowed to update
     * working memory voluntarily but the loop won't nag it.
     */
    fun getReminders(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMINDERS, DEFAULT_REMINDERS)

    fun setReminders(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REMINDERS, enabled).apply()
    }
}
