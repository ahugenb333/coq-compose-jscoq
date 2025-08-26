package dev.cohere.coq

import android.content.Intent
import android.net.Uri
import dev.cohere.coq.nav.Route

fun Intent.asStartRoute(): Route? {
    val u: Uri = this.data ?: return null
    val path = u.path?.trimStart('/') ?: ""
    val q = u.queryParameterNames.associateWith { key -> u.getQueryParameter(key) }
    return when (path) {
        "open" -> Route.Open(uri = q["uri"], text = q["text"])
        "prove" -> Route.Prove(uri = q["uri"], text = q["text"], profile = q["profile"])
        else -> null
    }
}
