package dev.cohere.coq.engine

import dev.cohere.coq.model.Goal
import dev.cohere.coq.model.ProveResult
import dev.cohere.coq.model.SuggestState

interface CoqEngine : AutoCloseable {
    suspend fun init()
    suspend fun open(uri: String, text: String)
    suspend fun peek(cursor: Int): SuggestState
    suspend fun applyTactic(tactic: String): Goal
    suspend fun prove(uri: String, profile: String, text: String): ProveResult
}
