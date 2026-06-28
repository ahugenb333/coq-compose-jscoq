package dev.cohere.coq.engine

import dev.cohere.coq.model.Goal
import dev.cohere.coq.model.ProveResult
import dev.cohere.coq.model.SuggestState
import dev.cohere.coq.model.TacticSuggestion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import com.multiplatform.webview.web.WebViewNavigator
import kotlin.time.Duration.Companion.seconds

class WebCoqEngine : CoqEngine {
    private val pending = mutableMapOf<Long, CompletableDeferred<String>>()
    private var seq = 1L
    private val ready = CompletableDeferred<Unit>()
    private val sendMutex = Mutex()

    internal var navigator: WebViewNavigator? = null

    internal fun onBridgeMessage(json: String) {
        val root = runCatching { Json.parseToJsonElement(json).jsonObject }.getOrNull() ?: return
        when (root["type"]?.toString()?.trim('"')) {
            "ready" -> if (!ready.isCompleted) ready.complete(Unit)
            "response" -> {
                val id = root["id"]?.toString()?.toLongOrNull() ?: return
                pending.remove(id)?.complete(json)
            }
        }
    }

    override suspend fun init() {
        withTimeout(15.seconds) { ready.await() }
    }

    override suspend fun open(uri: String, text: String) {
        eval(EngineCmd(id = nextId(), type = "open", uri = uri, text = text))
    }

    override suspend fun peek(cursor: Int): SuggestState {
        val res = eval(EngineCmd(id = nextId(), type = "peek", cursor = cursor))
        val payload = Json.parseToJsonElement(res).jsonObject["payload"]!!.jsonObject
        val goalObj = payload["goal"]!!.jsonObject
        val ty = goalObj["ty"]!!.toString().trim('"')
        val ctx = goalObj["ctx"]?.jsonArray?.map { it.toString().trim('"') } ?: emptyList()
        val tactics = payload["tactics"]?.jsonArray?.map {
            val o = it.jsonObject
            TacticSuggestion(o["t"]!!.toString().trim('"'), o["score"]!!.toString().toDouble())
        } ?: emptyList()
        return SuggestState(Goal(ty, ctx), tactics)
    }

    override suspend fun applyTactic(tactic: String): Goal {
        val res = eval(EngineCmd(id = nextId(), type = "exec", text = tactic))
        val payload = Json.parseToJsonElement(res).jsonObject["payload"]!!.jsonObject
        val goalObj = payload["goal"]!!.jsonObject
        return Goal(goalObj["ty"]!!.toString().trim('"'))
    }

    override suspend fun prove(uri: String, profile: String, text: String): ProveResult {
        val res = eval(
            EngineCmd(
                id = nextId(),
                type = "prove",
                uri = uri,
                text = text,
                profile = profile,
            ),
        )
        val payload = Json.parseToJsonElement(res).jsonObject["payload"]!!.jsonObject
        return ProveResult(
            ok = payload["ok"]?.toString()?.trim() == "true",
            script = payload["script"]?.toString()?.trim('"'),
            alt = payload["alt"]?.jsonArray?.map { it.toString().trim('"') } ?: emptyList(),
            confidence = payload["confidence"]?.toString()?.toDoubleOrNull() ?: 0.0,
        )
    }

    private fun nextId(): Long = seq++

    private suspend fun eval(cmd: EngineCmd): String = sendMutex.withLock {
        val nav = navigator ?: error("Coq WebView is not attached yet")
        val def = CompletableDeferred<String>()
        pending[cmd.id] = def
        val json = Json.encodeToString(EngineCmd.serializer(), cmd).replace("\n", "\\n")
        nav.evaluateJavaScript("window.dispatchCoq($json);")
        withTimeout(10.seconds) { def.await() }
    }

    override fun close() = Unit
}

@Serializable
internal data class EngineCmd(
    val id: Long,
    val type: String,
    val uri: String? = null,
    val text: String? = null,
    val cursor: Int? = null,
    val profile: String? = null,
)
