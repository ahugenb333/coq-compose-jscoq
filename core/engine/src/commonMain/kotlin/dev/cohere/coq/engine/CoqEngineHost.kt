package dev.cohere.coq.engine

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.multiplatform.webview.jsbridge.rememberWebViewJsBridge
import com.multiplatform.webview.web.LoadingState
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewNavigator
import com.multiplatform.webview.web.rememberWebViewStateWithHTMLFile
import kotlinx.coroutines.flow.filter

@Composable
fun rememberWebCoqEngine(): WebCoqEngine = remember { WebCoqEngine() }

@Composable
fun CoqEngineHost(
    engine: WebCoqEngine,
    modifier: Modifier = Modifier,
) {
    val webViewState = rememberWebViewStateWithHTMLFile(fileName = "index.html")
    val navigator = rememberWebViewNavigator()
    val jsBridge = rememberWebViewJsBridge(navigator)

    LaunchedEffect(jsBridge) {
        jsBridge.register(CoqMessageHandler(engine))
    }

    LaunchedEffect(webViewState) {
        webViewState.webSettings.apply {
            isJavaScriptEnabled = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            androidWebSettings.apply {
                allowFileAccess = true
            }
        }
    }

    LaunchedEffect(navigator, webViewState) {
        engine.navigator = navigator
        snapshotFlow { webViewState.loadingState }
            .filter { it is LoadingState.Finished }
            .collect { /* page loaded; bridge.js sends ready via CoqMessage */ }
    }

    WebView(
        state = webViewState,
        modifier = modifier.size(1.dp),
        captureBackPresses = false,
        navigator = navigator,
        webViewJsBridge = jsBridge,
    )
}
