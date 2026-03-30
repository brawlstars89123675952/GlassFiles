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
import java.util.zip.ZipFile

// ═══════════════════════════════════
// AI Models — Gemini + Qwen
// ═══════════════════════════════════

enum class AiProvider(
    val label: String, val modelId: String, val supportsVision: Boolean,
    val desc: String, val isGemini: Boolean = false, val isQwen: Boolean = false,
    val supportsFiles: Boolean = false
) {
    // Gemini
    GEMINI_FLASH("Gemini 2.5 Flash", "gemini-2.5-flash", true, "Fast, efficient", isGemini = true),
    GEMINI_PRO("Gemini 2.5 Pro", "gemini-2.5-pro", true, "Most capable", isGemini = true),
    GEMINI_FLASH_LITE("Gemini 2.5 Flash-Lite", "gemini-2.5-flash-lite", true, "Lightweight", isGemini = true),
    GEMINI_3_FLASH("Gemini 3 Flash", "gemini-3-flash-preview", true, "Next-gen fast", isGemini = true),
    GEMINI_31_PRO("Gemini 3.1 Pro", "gemini-3.1-pro-preview", true, "Next-gen pro", isGemini = true),

    // Qwen
    QWEN_PLUS("Qwen Plus", "qwen-plus", false, "Balanced, smart", isQwen = true, supportsFiles = true),
    QWEN_MAX("Qwen Max", "qwen-max", false, "Most powerful", isQwen = true, supportsFiles = true),
    QWEN_TURBO("Qwen Turbo", "qwen-turbo", false, "Fast, cheap", isQwen = true, supportsFiles = true),
    QWEN_VL_PLUS("Qwen VL Plus", "qwen-vl-plus", true, "Vision + files", isQwen = true, supportsFiles = true),
    QWEN_LONG("Qwen Long", "qwen-long", false, "10M context", isQwen = true, supportsFiles = true),
}

data class ChatMessage(val role: String, val content: String, val imageBase64: String? = null, val fileContent: String? = null)

// ═══════════════════════════════════
// API Key Storage
// ═══════════════════════════════════

object GeminiKeyStore {
    private const val PREFS = "gemini_prefs"
    private const val KEY_GEMINI = "api_key"
    private const val KEY_PROXY = "proxy_url"
    private const val KEY_QWEN = "qwen_api_key"
    private const val KEY_QWEN_REGION = "qwen_region"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Gemini
    fun getKey(context: Context): String = prefs(context).getString(KEY_GEMINI, "") ?: ""
    fun saveKey(context: Context, key: String) = prefs(context).edit().putString(KEY_GEMINI, key.trim()).apply()
    fun hasKey(context: Context): Boolean = getKey(context).isNotBlank()

    fun getProxy(context: Context): String = prefs(context).getString(KEY_PROXY, "") ?: ""
    fun saveProxy(context: Context, url: String) = prefs(context).edit().putString(KEY_PROXY, url.trim().trimEnd('/')).apply()

    // Qwen
    fun getQwenKey(context: Context): String = prefs(context).getString(KEY_QWEN, "") ?: ""
    fun saveQwenKey(context: Context, key: String) = prefs(context).edit().putString(KEY_QWEN, key.trim()).apply()
    fun hasQwenKey(context: Context): Boolean = getQwenKey(context).isNotBlank()

    // Qwen region: "intl" (Singapore) or "cn" (Beijing)
    fun getQwenRegion(context: Context): String = prefs(context).getString(KEY_QWEN_REGION, "intl") ?: "intl"
    fun saveQwenRegion(context: Context, region: String) = prefs(context).edit().putString(KEY_QWEN_REGION, region).apply()
}

// ═══════════════════════════════════
// AI Manager
// ═══════════════════════════════════

object AiManager {
    private const val GEMINI_BASE = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val SYSTEM_PROMPT = "You are a helpful AI assistant in the GlassFiles app — a file manager for Android. You can analyze files, code, images, and archives. Respond in the same language as the user."

    private fun qwenBaseUrl(region: String): String = when (region) {
        "cn" -> "https://dashscope.aliyuncs.com/compatible-mode/v1"
        else -> "https://dashscope-intl.aliyuncs.com/compatible-mode/v1"
    }

    suspend fun chat(
        provider: AiProvider,
        messages: List<ChatMessage>,
        geminiKey: String = "",
        openRouterKey: String = "", // unused, kept for compat
        proxyUrl: String = "",
        qwenKey: String = "",
        qwenRegion: String = "intl",
        onChunk: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        when {
            provider.isGemini -> {
                if (geminiKey.isBlank()) throw Exception("Enter Gemini API key in AI settings")
                doChatGemini(provider.modelId, messages, geminiKey, proxyUrl, onChunk)
            }
            provider.isQwen -> {
                if (qwenKey.isBlank()) throw Exception("Enter Qwen API key in AI settings")
                doChatQwen(provider.modelId, messages, provider.supportsVision, qwenKey, qwenRegion, onChunk)
            }
            else -> throw Exception("Unknown provider")
        }
    }

    // ═══════════════════════════════════
    // Qwen (OpenAI-compatible streaming)
    // ═══════════════════════════════════

    private fun doChatQwen(
        modelId: String, messages: List<ChatMessage>, supportsVision: Boolean,
        apiKey: String, region: String, onChunk: (String) -> Unit
    ): String {
        val url = "${qwenBaseUrl(region)}/chat/completions"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            doOutput = true; connectTimeout = 30000; readTimeout = 120000
        }

        val msgs = JSONArray()
        msgs.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))

        messages.forEach { msg ->
            when {
                // Vision message with image
                msg.imageBase64 != null && supportsVision -> {
                    val content = JSONArray()
                    content.put(JSONObject().put("type", "text").put("text", msg.content))
                    content.put(JSONObject().put("type", "image_url").put("image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,${msg.imageBase64}")))
                    msgs.put(JSONObject().put("role", msg.role).put("content", content))
                }
                // Message with file content attached
                msg.fileContent != null -> {
                    val fullText = if (msg.content.isNotBlank()) "${msg.content}\n\n--- File content ---\n${msg.fileContent}" else msg.fileContent
                    msgs.put(JSONObject().put("role", msg.role).put("content", fullText))
                }
                // Regular text
                else -> msgs.put(JSONObject().put("role", msg.role).put("content", msg.content))
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
            val err = (if (code >= 400) conn.errorStream else conn.inputStream)?.bufferedReader()?.readText()?.take(500) ?: "error $code"
            conn.disconnect()
            val detail = try { JSONObject(err).optJSONObject("error")?.optString("message", "") ?: err.take(200) } catch (_: Exception) { err.take(200) }
            throw Exception("Qwen $code: $detail")
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
    // Gemini (SSE streaming)
    // ═══════════════════════════════════

    private fun doChatGemini(
        modelId: String, messages: List<ChatMessage>,
        apiKey: String, proxyUrl: String, onChunk: (String) -> Unit
    ): String {
        val base = proxyUrl.ifBlank { GEMINI_BASE }
        val url = "$base/$modelId:streamGenerateContent?alt=sse&key=$apiKey"

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; setRequestProperty("Content-Type", "application/json")
            doOutput = true; connectTimeout = 30000; readTimeout = 120000
        }

        val contents = JSONArray()
        messages.forEach { msg ->
            val role = if (msg.role == "assistant") "model" else "user"
            val parts = JSONArray()
            if (msg.content.isNotBlank()) parts.put(JSONObject().put("text", msg.content))
            if (msg.fileContent != null) parts.put(JSONObject().put("text", "\n--- File content ---\n${msg.fileContent}"))
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
            val err = (if (code >= 400) conn.errorStream else conn.inputStream)?.bufferedReader()?.readText()?.take(800) ?: "error $code"
            conn.disconnect()
            val googleMsg = try { JSONObject(err).optJSONObject("error")?.optString("message", "") ?: "" } catch (_: Exception) { "" }
            val detail = googleMsg.ifBlank { err.take(200) }
            when (code) {
                400 -> throw Exception("400: $detail")
                403 -> throw Exception("403 Access denied: $detail")
                404 -> throw Exception("Model $modelId not found")
                429 -> throw Exception("429 Rate limit: $detail")
                else -> throw Exception("$code: $detail")
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
                val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: continue
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
    // ZIP Archive Support
    // ═══════════════════════════════════

    /** Extract ZIP contents to temp dir, return list of (name, content) */
    fun extractZipForAi(zipPath: String, context: Context, maxFiles: Int = 20, maxCharsPerFile: Int = 6000): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        val tempDir = File(context.cacheDir, "ai_zip_${System.currentTimeMillis()}")
        try {
            tempDir.mkdirs()
            val zip = ZipFile(File(zipPath))
            var count = 0
            zip.entries().asSequence()
                .filter { !it.isDirectory }
                .filter { !it.name.endsWith(".exe") && !it.name.endsWith(".dll") && !it.name.endsWith(".so") && !it.name.endsWith(".bin") }
                .take(maxFiles)
                .forEach { entry ->
                    val ext = entry.name.substringAfterLast(".", "").lowercase()
                    val isText = ext in listOf(
                        "txt", "md", "json", "xml", "html", "css", "js", "ts", "kt", "java",
                        "py", "c", "cpp", "h", "swift", "go", "rs", "rb", "php", "sh", "bash",
                        "yml", "yaml", "toml", "ini", "cfg", "conf", "properties", "gradle",
                        "csv", "sql", "dart", "lua", "r", "scala", "jsx", "tsx", "vue", "svelte",
                        "log", "env", "gitignore", "dockerfile", "makefile"
                    )
                    val isImage = ext in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

                    if (isText && entry.size < 500_000) {
                        try {
                            val text = zip.getInputStream(entry).bufferedReader().readText()
                            val truncated = if (text.length > maxCharsPerFile) text.take(maxCharsPerFile) + "\n...[truncated]" else text
                            results.add(Pair(entry.name, truncated))
                        } catch (_: Exception) { results.add(Pair(entry.name, "[read error]")) }
                    } else if (isImage) {
                        results.add(Pair(entry.name, "[image: ${entry.size / 1024}KB]"))
                    } else {
                        results.add(Pair(entry.name, "[binary: ${ext.uppercase()}, ${entry.size / 1024}KB]"))
                    }
                    count++
                }
            zip.close()
            val totalEntries = ZipFile(File(zipPath)).use { z -> z.entries().asSequence().count() }
            if (totalEntries > maxFiles) {
                results.add(Pair("...", "[${totalEntries - maxFiles} more files not shown]"))
            }
        } catch (e: Exception) {
            results.add(Pair("error", "Failed to read ZIP: ${e.message}"))
        } finally {
            tempDir.deleteRecursively()
        }
        return results
    }

    /** Format ZIP contents as text for AI */
    fun formatZipContents(entries: List<Pair<String, String>>): String {
        val sb = StringBuilder("Archive contents:\n\n")
        entries.forEach { (name, content) ->
            sb.append("=== $name ===\n")
            sb.append(content)
            sb.append("\n\n")
        }
        return sb.toString()
    }

    // ═══════════════════════════════════
    // File Reading
    // ═══════════════════════════════════

    fun encodeImage(file: File): String? = try {
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

    fun readFileForAi(file: File, maxChars: Int = 8000): String? = try {
        if (file.length() > 500_000) "[File too large: ${file.length() / 1024}KB]"
        else {
            val text = file.readText()
            if (text.length > maxChars) text.take(maxChars) + "\n...[truncated, ${text.length} chars]" else text
        }
    } catch (_: Exception) { null }

    /** Read any supported file — text, image, zip */
    fun readAnyFile(file: File, context: Context): FileReadResult {
        val ext = file.extension.lowercase()
        return when {
            ext in listOf("zip", "jar") -> {
                val entries = extractZipForAi(file.absolutePath, context)
                FileReadResult(type = "zip", textContent = formatZipContents(entries), fileName = file.name)
            }
            ext in listOf("png", "jpg", "jpeg", "gif", "webp", "bmp") -> {
                val b64 = encodeImage(file)
                FileReadResult(type = "image", imageBase64 = b64, fileName = file.name)
            }
            ext in listOf("exe", "dll", "so", "bin", "apk", "aab", "dex") -> {
                FileReadResult(type = "binary", textContent = "[Unsupported binary format: ${ext.uppercase()}, ${file.length() / 1024}KB]", fileName = file.name)
            }
            else -> {
                val text = readFileForAi(file)
                FileReadResult(type = "text", textContent = text, fileName = file.name)
            }
        }
    }
}

data class FileReadResult(
    val type: String, // "text", "image", "zip", "binary"
    val textContent: String? = null,
    val imageBase64: String? = null,
    val fileName: String = ""
)
