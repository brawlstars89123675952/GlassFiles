package com.glassfiles.data.ai.models

/**
 * Universal representation of a single AI model returned by some provider.
 *
 * - [providerId] tells the dispatcher which provider implementation to call.
 * - [id] is the canonical id used in API requests (e.g. `gpt-4o`, `claude-3-5-sonnet-20241022`,
 *   `gemini-2.5-pro`, `qwen3-max`, `moonshot-v1-128k`, `grok-2`).
 * - [displayName] is what to show in pickers; for many providers this is just [id]
 *   normalised (`gpt-4o` -> `GPT-4o`).
 * - [capabilities] is the inferred set of capabilities (see [CapabilityClassifier]).
 * - [contextWindow] is the max input tokens, when known.
 * - [deprecated] models are still listed but de-emphasised in pickers.
 */
data class AiModel(
    val providerId: AiProviderId,
    val id: String,
    val displayName: String,
    val capabilities: Set<AiCapability>,
    val contextWindow: Int? = null,
    val deprecated: Boolean = false,
) {
    /** Stable globally-unique id used as a key in storage / chat sessions. */
    val uniqueKey: String get() = "${providerId.name}:${id}"

    fun has(capability: AiCapability): Boolean = capability in capabilities
}
