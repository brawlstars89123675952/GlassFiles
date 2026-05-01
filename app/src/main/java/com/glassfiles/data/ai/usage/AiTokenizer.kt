package com.glassfiles.data.ai.usage

import com.glassfiles.data.ai.models.AiMessage
import com.glassfiles.data.ai.models.AiProviderId
import kotlin.math.ceil

/**
 * Provider-aware token counting abstraction. These implementations are
 * intentionally lightweight: exact mobile tokenizers are large and provider
 * specific, so unsupported families use conservative estimates plus runtime
 * calibration from provider-reported usage.
 */
interface Tokenizer {
    val name: String
    fun countTokens(text: String): Int

    fun countMessages(messages: List<AiMessage>): Int =
        messages.sumOf { message ->
            countTokens(message.content) +
                countTokens(message.fileContent.orEmpty()) +
                ((message.imageBase64?.length ?: 0) / IMAGE_TOKEN_CHAR_RATIO) +
                (message.toolCalls?.sumOf { countTokens(it.name) + countTokens(it.argsJson) } ?: 0)
        }
}

object TiktokenTokenizer : Tokenizer {
    override val name: String = "tiktoken-cl100k-estimate"
    override fun countTokens(text: String): Int = estimateByChars(text, englishCharsPerToken = 4.0, nonAsciiCharsPerToken = 2.2)
}

object ClaudeTokenizer : Tokenizer {
    override val name: String = "claude-estimate"
    override fun countTokens(text: String): Int = ceil(TiktokenTokenizer.countTokens(text) * 1.05).toInt()
}

object QwenTokenizer : Tokenizer {
    override val name: String = "qwen-char-estimate"
    override fun countTokens(text: String): Int = estimateByChars(text, englishCharsPerToken = 3.4, nonAsciiCharsPerToken = 1.8)
}

object GeminiTokenizer : Tokenizer {
    override val name: String = "gemini-char-estimate"
    override fun countTokens(text: String): Int = estimateByChars(text, englishCharsPerToken = 3.6, nonAsciiCharsPerToken = 2.0)
}

object DefaultTokenizer : Tokenizer {
    override val name: String = "default-conservative-estimate"
    override fun countTokens(text: String): Int = estimateByChars(text, englishCharsPerToken = 3.0, nonAsciiCharsPerToken = 1.8)
}

object TokenizerRegistry {
    fun forProvider(providerId: String, modelId: String): Tokenizer {
        val provider = providerId.lowercase()
        val model = modelId.lowercase()
        return when {
            provider == AiProviderId.OPENAI.name.lowercase() || provider == "openai" -> TiktokenTokenizer
            provider == AiProviderId.ANTHROPIC.name.lowercase() || provider == "anthropic" -> ClaudeTokenizer
            provider == AiProviderId.GOOGLE.name.lowercase() || provider == "google" -> GeminiTokenizer
            provider == AiProviderId.XAI.name.lowercase() || provider == "xai" -> TiktokenTokenizer
            provider == AiProviderId.ALIBABA.name.lowercase() || provider == "alibaba" -> QwenTokenizer
            provider == AiProviderId.MOONSHOT.name.lowercase() || provider == "moonshot" -> QwenTokenizer
            provider == AiProviderId.OPENROUTER.name.lowercase() || provider == "openrouter" -> when {
                "gpt" in model || model.startsWith("openai/") -> TiktokenTokenizer
                "claude" in model || model.startsWith("anthropic/") -> ClaudeTokenizer
                "gemini" in model || model.startsWith("google/") -> GeminiTokenizer
                "qwen" in model || "kimi" in model || "moonshot" in model -> QwenTokenizer
                "grok" in model || model.startsWith("x-ai/") || model.startsWith("xai/") -> TiktokenTokenizer
                else -> DefaultTokenizer
            }
            else -> DefaultTokenizer
        }
    }
}

private fun estimateByChars(
    text: String,
    englishCharsPerToken: Double,
    nonAsciiCharsPerToken: Double,
): Int {
    if (text.isEmpty()) return 0
    var ascii = 0
    var nonAscii = 0
    text.forEach { ch -> if (ch.code <= 0x7F) ascii++ else nonAscii++ }
    return ceil(ascii / englishCharsPerToken + nonAscii / nonAsciiCharsPerToken).toInt().coerceAtLeast(1)
}

private const val IMAGE_TOKEN_CHAR_RATIO = 8
