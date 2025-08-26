package dev.cohere.coq.engine

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.cohere.coq.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class CoqEngine(context: Context) : AutoCloseable {
    private val webView: WebView = WebView(context)
    private val pending = ConcurrentHashMap<Long, CompletableDeferred<String>>()
    private val seq = AtomicLong(1)
    private val ready = CompletableDeferred<Boolean>()
    private val sendMutex = Mutex()

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun init() = withContext(Dispatchers.Main) {
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(CoqBridge(
            onMessage = { json ->
                if (json.contains(""type":"ready"")) {
                    if (!ready.isCompleted) ready.complete(true)
                } else {
                    val id = Regex("\"id\":(\-?\d+)").find(json)?.groupValues?.get(1)?.toLongOrNull()
                    if (id != null) pending.remove(id)?.complete(json)
                }
            }
        ), "Android")
        webView.webViewClient = object : WebViewClient() {}
        webView.loadUrl("file:///android_asset/index.html")
        withTimeout(5.seconds) { ready.await() }
        Unit
    }

    suspend fun open(uri: String, text: String) {
        eval(EngineCmd(id = seq.getAndIncrement(), type = "open", uri = uri, text = text))
    }

    suspend fun peek(cursor: Int): SuggestState {
        val res = eval(EngineCmd(id = seq.getAndIncrement(), type = "peek", cursor = cursor))
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

    suspend fun applyTactic(tactic: String): Goal {
        val res = eval(EngineCmd(id = seq.getAndIncrement(), type = "exec", text = tactic))
        val payload = Json.parseToJsonElement(res).jsonObject["payload"]!!.jsonObject
        val goalObj = payload["goal"]!!.jsonObject
        return Goal(goalObj["ty"]!!.toString().trim('"'))
    }

    suspend fun prove(uri: String, profile: String, text: String): ProveResult {
        val res = eval(EngineCmd(id = seq.getAndIncrement(), type = "prove", uri = uri, text = text, profile = profile))
        val payload = Json.parseToJsonElement(res).jsonObject["payload"]!!.jsonObject
        return ProveResult(
            ok = payload["ok"]?.toString()?.trim() == "true",
            script = payload["script"]?.toString()?.trim('"'),
            alt = payload["alt"]?.jsonArray?.map { it.toString().trim('"') } ?: emptyList(),
            confidence = payload["confidence"]?.toString()?.toDoubleOrNull() ?: 0.0
        )
    }

    private suspend fun eval(cmd: EngineCmd, timeout: Duration = 5.seconds): String = withContext(Dispatchers.Main) {
        sendMutex.withLock {
            val def = CompletableDeferred<String>()
            pending[cmd.id] = def
            val json = Json.encodeToString(EngineCmd.serializer(), cmd).replace("\n", "\\n")
            webView.evaluateJavascript("window.dispatchCoq($json);", null)
            withTimeout(timeout) { def.await() }
        }
    }

    override fun close() {
        webView.destroy()
    }
}

class CoqBridge(private val onMessage: (String) -> Unit) {
    @JavascriptInterface
    fun onMessage(json: String) { onMessage(json) }
}

@Serializable
data class EngineCmd(
    val id: Long,
    val type: String,
    val uri: String? = null,
    val text: String? = null,
    val cursor: Int? = null,
    val profile: String? = null
)
