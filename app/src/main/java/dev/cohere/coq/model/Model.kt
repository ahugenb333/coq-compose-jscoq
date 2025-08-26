package dev.cohere.coq.model

import kotlinx.serialization.Serializable

@Serializable data class Goal(val ty: String, val ctx: List<String> = emptyList())
@Serializable data class SuggestState(val goal: Goal, val tactics: List<TacticSuggestion> = emptyList())
@Serializable data class TacticSuggestion(val t: String, val score: Double = 0.0)

@Serializable data class ProveResult(
    val ok: Boolean,
    val script: String?,
    val alt: List<String> = emptyList(),
    val confidence: Double = 0.0
)
