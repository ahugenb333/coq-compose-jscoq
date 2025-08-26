package dev.cohere.coq.nav

sealed interface Route {
    val path: String
    data class Open(val uri: String?, val text: String?) : Route { override val path = "open" }
    data class Prove(val uri: String?, val text: String?, val profile: String?) : Route { override val path = "prove" }
}
