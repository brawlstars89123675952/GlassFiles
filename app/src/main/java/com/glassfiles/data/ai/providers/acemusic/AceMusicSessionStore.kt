package com.glassfiles.data.ai.providers.acemusic

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object AceMusicSessionStore {
    private const val PREFS = "acemusic_session"
    private const val KEY_AUTHORIZATION = "authorization"
    private const val KEY_COOKIE = "cookie"
    private const val KEY_USER_AGENT = "user_agent"
    private const val KEY_UPDATED_AT = "updated_at"

    const val KEY_MARKER = """{"session":"acemusic"}"""
    const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    fun save(
        context: Context,
        authorization: String,
        cookie: String = "",
        userAgent: String = DEFAULT_USER_AGENT,
    ) {
        prefs(context).edit()
            .putString(KEY_AUTHORIZATION, authorization.trim())
            .putString(KEY_COOKIE, cookie.trim())
            .putString(KEY_USER_AGENT, userAgent.trim().ifBlank { DEFAULT_USER_AGENT })
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun hasSession(context: Context): Boolean =
        prefs(context).getString(KEY_AUTHORIZATION, "").orEmpty().isNotBlank()

    fun headers(context: Context): Map<String, String> {
        val prefs = prefs(context)
        val authorization = prefs.getString(KEY_AUTHORIZATION, "").orEmpty()
        if (authorization.isBlank()) return emptyMap()
        val headers = linkedMapOf(
            "Authorization" to authorization,
            "User-Agent" to prefs.getString(KEY_USER_AGENT, DEFAULT_USER_AGENT).orEmpty().ifBlank { DEFAULT_USER_AGENT },
            "Origin" to "https://acemusic.ai",
            "Referer" to "https://acemusic.ai/",
        )
        prefs.getString(KEY_COOKIE, "").orEmpty().takeIf { it.isNotBlank() }?.let {
            headers["Cookie"] = it
        }
        return headers
    }

    private fun prefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            appContext,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }
}
