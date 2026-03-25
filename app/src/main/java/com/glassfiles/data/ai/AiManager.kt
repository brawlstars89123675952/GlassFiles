package com.glassfiles.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// ═══════════════════════════════════
// Провайдеры
// ═══════════════════════════════════

enum class AiProvider(val label: String, val modelId: String, val supportsVision: Boolean, val isGemini: Boolean = false) {
    // OpenRouter (бесплатные)
    AUTO("Auto (recommended)", "openrouter/free", false),
    LLAMA33("Llama 3.3 70B", "meta-llama/llama-3.3-70b-instruct:free", false),
    DEEPSEEK_R1("DeepSeek R1", "deepseek/deepseek-r1-0528:free", false),
    QWEN_CODER("Qwen3 Coder 480B", "qwen/qwen3-coder-480b-a35b:free", false),
    NEMOTRON("Nemotron Nano 8B", "nvidia/llama-3.1-nemotron-nano-8b-v1:free", false),
    GEMMA("Gemma 3 27B", "google/gemma-3-27b-it:free", true),

    // Gemini (нужен API ключ) — актуальные модели март 2026
    GEMINI_FLASH("Gemini 2.5 Flash", "gemini-2.5-flash", true, true),
    GEMINI_PRO("Gemini 2.5 Pro", "gemini-2.5-pro", true, true),
    GEMINI_3_FLASH("Gemini 3 Flash", "gemini-3-flash-preview", true, true),
    GEMINI_31_PRO("Gemini 3.1 Pro", "gemini-3.1-pro-preview", true, true),
    GEMINI_FLASH_LITE("Gemini 2.5 Flash-Lite", "gemini-2.5-flash-lite", true, true),
}

data class ChatMessage(val role: String, val content: String, val imageBase64: String? = null)

// ═══════════════════════════════════
// Хранение API ключей и прокси
// ═══════════════════════════════════

object GeminiKeyStore {
    private const val PREFS = "gemini_prefs"
    private const val KEY_GEMINI = "api_key"
    private const val KEY_OPENROUTER = "openrouter_key"
    private const val KEY_PROXY = "proxy_url"

    // Дефолты — работают, но пользователь может заменить
    private const val DEFAULT_GEMINI = "AIzaSyDvITHszY1xt6R2kvc6Pv6sEgVHzjAQyJE"
    private const val DEFAULT_OPENROUTER = "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"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Gemini
    fun getKey(context: Context): String {
        val saved = prefs(context).getString(KEY_GEMINI, "") ?: ""
        return saved.ifBlank { DEFAULT_GEMINI }
    }
    fun saveKey(context: Context, key: String) = prefs(context).edit().putString(KEY_GEMINI, key.trim()).apply()
    fun hasKey(context: Context): Boolean = getKey(context).isNotBlank()

    // OpenRouter
    fun getOpenRouterKey(context: Context): String {
        val saved = prefs(context).getString(KEY_OPENROUTER, "") ?: ""
        return saved.ifBlank { DEFAULT_OPENROUTER }
    }
    fun saveOpenRouterKey(context: Context, key: String) = prefs(context).edit().putString(KEY_OPENROUTER, key.trim()).apply()

    // Прокси для Gemini (напр. https://my-proxy.com/v1beta/models)
    fun getProxy(context: Context): String = prefs(context).getString(KEY_PROXY, "") ?: ""
    fun saveProxy(context: Context, url: String) = prefs(context).edit().putString(KEY_PROXY, url.trim().trimEnd('/')).apply()
}

// ═══════════════════════════════════
// AI Manager
// ═══════════════════════════════════

object AiManager {
    private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    private val fallbackOrder = listOf(
        "openrouter/free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "deepseek/deepseek-r1-0528:free",
        "nvidia/llama-3.1-nemotron-nano-8b-v1:free",
        "qwen/qwen3-coder-480b-a35b:free",
        "google/gemma-3-27b-it:free"
    )

    private const val SYSTEM_PROMPT = "You are a helpful AI assistant in the GlassFiles app — a file manager for Android. Respond in the same language as the user."

    suspend fun chat(
        provider: AiProvider,
        messages: List<ChatMessage>,
        geminiKey: String = "",
        openRouterKey: String = "",
        proxyUrl: String = "",
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        if (provider.isGemini) {
            if (geminiKey.isBlank()) throw Exception("Enter Gemini API key in settings")
            return@withContext doChatGemini(provider.modelId, messages, geminiKey, proxyUrl, onChunk)
        }

        val orKey = openRouterKey.ifBlank { throw Exception("Enter OpenRouter API key in settings") }

        // OpenRouter — с фоллбеком
        val modelsToTry = mutableListOf(provider.modelId)
        if (provider != AiProvider.AUTO) {
            modelsToTry.addAll(fallbackOrder.filter { it != provider.modelId })
        }

        var lastError: Exception? = null
        for (model in modelsToTry) {
            try {
                return@withContext doChatOpenRouter(model, messages, provider.supportsVision, orKey, onChunk)
            } catch (e: Exception) {
                lastError = e
                if (!e.message.orEmpty().contains("404")) throw e
            }
        }
        throw lastError ?: Exception("All models unavailable")
    }

    // ═══════════════════════════════════
    // Gemini REST API (streaming SSE)
    // ═══════════════════════════════════

    private fun doChatGemini(
        modelId: String,
        messages: List<ChatMessage>,
        apiKey: String,
        proxyUrl: String,
        onChunk: (String) -> Unit
    ): String {
        val base = proxyUrl.ifBlank { GEMINI_BASE }
        val url = "$base/$modelId:streamGenerateContent?alt=sse&key=$apiKey"

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 120000
        }

        // Формируем contents в формате Gemini
        val contents = JSONArray()
        messages.forEach { msg ->
            val role = if (msg.role == "assistant") "model" else "user"
            val parts = JSONArray()

            // Текст
            if (msg.content.isNotBlank()) {
                parts.put(JSONObject().put("text", msg.content))
            }

            // Изображение
            if (msg.imageBase64 != null) {
                parts.put(JSONObject().put("inlineData",
                    JSONObject().put("mimeType", "image/jpeg").put("data", msg.imageBase64)))
            }

            contents.put(JSONObject().put("role", role).put("parts", parts))
        }

        val body = JSONObject()
            .put("contents", contents)
            .put("systemInstruction", JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .toString()

        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = (if (code >= 400) conn.errorStream else conn.inputStream)
                ?.bufferedReader()?.readText()?.take(800) ?: "error $code"
            conn.disconnect()

            // Парсим сообщение из JSON ответа Google
            val googleMsg = try {
                val errJson = JSONObject(err)
                errJson.optJSONObject("error")?.optString("message", "") ?: ""
            } catch (_: Exception) { "" }

            val detail = googleMsg.ifBlank { err.take(200) }

            when (code) {
                400 -> throw Exception("Gemini 400: $detail")
                403 -> throw Exception("Gemini 403 (доступ запрещён): $detail")
                404 -> throw Exception("Model $modelId not found. Check the name.")
                429 -> throw Exception("Gemini 429: $detail")
                else -> throw Exception("Gemini $code: $detail")
            }
        }

        val sb = StringBuilder()
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            if (data == "[DONE]" || data.isEmpty()) continue
            try {
                val json = JSONObject(data)
                val candidates = json.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts") ?: continue
                for (i in 0 until parts.length()) {
                    val text = parts.getJSONObject(i).optString("text", "")
                    if (text.isNotEmpty()) { sb.append(text); onChunk(text) }
                }
            } catch (_: Exception) {}
        }
        reader.close(); conn.disconnect()
        return sb.toString()
    }

    // ═══════════════════════════════════
    // OpenRouter (OpenAI-совместимый)
    // ═══════════════════════════════════

    private fun doChatOpenRouter(
        modelId: String,
        messages: List<ChatMessage>,
        supportsVision: Boolean,
        apiKey: String,
        onChunk: (String) -> Unit
    ): String {
        val conn = (URL(OPENROUTER_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("HTTP-Referer", "https://glassfiles.app")
            setRequestProperty("X-Title", "GlassFiles")
            doOutput = true
            connectTimeout = 30000
            readTimeout = 120000
        }

        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))

        messages.forEach { msg ->
            if (msg.imageBase64 != null && supportsVision) {
                val content = JSONArray()
                content.put(JSONObject().put("type", "text").put("text", msg.content))
                content.put(JSONObject().put("type", "image_url").put("image_url",
                    JSONObject().put("url", "data:image/jpeg;base64,${msg.imageBase64}")))
                msgs.put(JSONObject().put("role", msg.role).put("content", content))
            } else {
                msgs.put(JSONObject().put("role", msg.role).put("content", msg.content))
            }
        }

        val body = JSONObject()
            .put("model", modelId)
            .put("messages", msgs)
            .put("stream", true)
            .toString()

        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        if (code !in 200..299) {
            val err = (if (code >= 400) conn.errorStream else conn.inputStream)
                ?.bufferedReader()?.readText()?.take(300) ?: "error $code"
            conn.disconnect()
            throw Exception("Error $code: $err")
        }

        val sb = StringBuilder()
        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            val l = line ?: continue
            if (!l.startsWith("data: ")) continue
            val data = l.removePrefix("data: ").trim()
            if (data == "[DONE]") break
            try {
                val chunk = JSONObject(data).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("delta").optString("content", "")
                if (chunk.isNotEmpty()) { sb.append(chunk); onChunk(chunk) }
            } catch (_: Exception) {}
        }
        reader.close(); conn.disconnect()
        return sb.toString()
    }

    // ═══════════════════════════════════
    // Утилиты
    // ═══════════════════════════════════

    fun encodeImage(file: File): String? {
        return try {
            val bmp = BitmapFactory.decodeFile(file.absolutePath) ?: return null
            val maxSide = 1024
            val scaled = if (bmp.width > maxSide || bmp.height > maxSide) {
                val ratio = minOf(maxSide.toFloat() / bmp.width, maxSide.toFloat() / bmp.height)
                Bitmap.createScaledBitmap(bmp, (bmp.width * ratio).toInt(), (bmp.height * ratio).toInt(), true)
            } else bmp
            val baos = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) { null }
    }

    fun readFileForAi(file: File, maxChars: Int = 8000): String? {
        return try {
            if (file.length() > 500_000) return "[Файл слишком большой: ${file.length() / 1024}КБ]"
            val text = file.readText()
            if (text.length > maxChars) text.take(maxChars) + "\n...[обрезано, ${text.length} символов]"
            else text
        } catch (_: Exception) { null }
    }
}
