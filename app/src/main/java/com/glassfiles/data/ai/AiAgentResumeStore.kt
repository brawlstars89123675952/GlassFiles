package com.glassfiles.data.ai

import android.content.Context
import org.json.JSONObject

/**
 * Tracks "did the last agent run finish, or was it interrupted?" so the
 * UI can offer a re-run when the user re-opens the screen after the
 * process was killed mid-task.
 *
 * Lifecycle:
 *  - `markStarted` is called the moment a new task launches in
 *    `runTaskInternal`. The user-visible message is *already* in the
 *    transcript by then; we only need to remember the task pointer
 *    (session id, the prompt that started it, repo / branch / model
 *    labels for sanity-check on restore).
 *  - `markFinished` is called in the run's `finally {}` block. From
 *    that moment forward `getPending` returns `null` for that session.
 *
 *  If the process is killed between `markStarted` and `markFinished`
 *  (OOM, swipe-from-recents, etc), the entry survives and the next
 *  open of the agent screen sees a non-null `getPending` and surfaces
 *  a "Resume?" banner.
 *
 * Design rationale:
 *  - One file per session so concurrent sessions don't clobber each
 *    other and a clean-up on session delete is just a file delete.
 *  - SharedPrefs (not full JSON-on-disk) — payload is tiny (≤ 1KB),
 *    no point in I/O ceremony. All access wrapped in `runCatching` so
 *    a corrupt write can't kill the agent loop.
 */
object AiAgentResumeStore {
    private const val PREFS = "ai_agent_resume"
    private const val KEY_PREFIX = "resume__"

    /**
     * @property prompt          the user input that kicked off the task.
     *                           Reused verbatim if the user taps Resume.
     * @property imageBase64     vision attachment, when one was sent.
     * @property repoFullName    `owner/name`. Used to validate the
     *                           pending entry still applies — if the
     *                           user has switched to a different repo
     *                           we silently drop it.
     * @property branch          active branch label.
     * @property modelKey        `AiModel.uniqueKey`. Same validation
     *                           reason as repo.
     * @property startedAt       wall-clock ms — purely informational.
     */
    data class Pending(
        val prompt: String,
        val imageBase64: String?,
        val repoFullName: String,
        val branch: String,
        val modelKey: String,
        val startedAt: Long,
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun markStarted(context: Context, sessionId: String, pending: Pending) {
        runCatching {
            val obj = JSONObject()
                .put("prompt", pending.prompt)
                .put("imageBase64", pending.imageBase64 ?: "")
                .put("repoFullName", pending.repoFullName)
                .put("branch", pending.branch)
                .put("modelKey", pending.modelKey)
                .put("startedAt", pending.startedAt)
            prefs(context).edit().putString(KEY_PREFIX + sessionId, obj.toString()).apply()
        }
    }

    fun markFinished(context: Context, sessionId: String) {
        runCatching {
            prefs(context).edit().remove(KEY_PREFIX + sessionId).apply()
        }
    }

    fun getPending(context: Context, sessionId: String): Pending? {
        val raw = prefs(context).getString(KEY_PREFIX + sessionId, null) ?: return null
        return runCatching {
            val obj = JSONObject(raw)
            Pending(
                prompt = obj.optString("prompt", ""),
                imageBase64 = obj.optString("imageBase64", "").takeIf { it.isNotBlank() },
                repoFullName = obj.optString("repoFullName", ""),
                branch = obj.optString("branch", ""),
                modelKey = obj.optString("modelKey", ""),
                startedAt = obj.optLong("startedAt", 0L),
            )
        }.getOrNull()
    }

    /** Wipe pending state without offering a resume. Used by the
     *  "Discard" button on the resume banner. */
    fun clear(context: Context, sessionId: String) {
        markFinished(context, sessionId)
    }
}
