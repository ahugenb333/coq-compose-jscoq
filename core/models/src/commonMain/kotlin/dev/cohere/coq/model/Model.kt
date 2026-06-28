package dev.cohere.coq.model

import kotlinx.serialization.Serializable

@Serializable
data class Goal(val ty: String, val ctx: List<String> = emptyList())

@Serializable
data class SuggestState(val goal: Goal, val tactics: List<TacticSuggestion> = emptyList())

@Serializable
data class TacticSuggestion(val t: String, val score: Double = 0.0)

@Serializable
data class ProveResult(
    val ok: Boolean,
    val script: String?,
    val alt: List<String> = emptyList(),
    val confidence: Double = 0.0,
)

const val DEFAULT_COQ_TEXT = """
Inductive nat : Set :=
| O : nat
| S : nat -> nat.

"""

const val DEFAULT_COQ_URI = "file:///Main.v"
