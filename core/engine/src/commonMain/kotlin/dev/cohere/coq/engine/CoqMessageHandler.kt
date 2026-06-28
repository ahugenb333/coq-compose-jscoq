package dev.cohere.coq.engine

import com.multiplatform.webview.jsbridge.IJsMessageHandler
import com.multiplatform.webview.jsbridge.JsMessage
import com.multiplatform.webview.web.WebViewNavigator

internal class CoqMessageHandler(
    private val engine: WebCoqEngine,
) : IJsMessageHandler {
    override fun methodName(): String = "CoqMessage"

    override fun handle(
        message: JsMessage,
        navigator: WebViewNavigator?,
        callback: (String) -> Unit,
    ) {
        engine.navigator = navigator ?: engine.navigator
        engine.onBridgeMessage(message.params)
        callback("{}")
    }
}
