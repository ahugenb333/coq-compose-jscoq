package dev.cohere.coq

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.cohere.coq.engine.CoqEngineHost
import dev.cohere.coq.engine.rememberWebCoqEngine
import dev.cohere.coq.feature.proof.ProofEditor
import dev.cohere.coq.ui.CoqTheme

@Composable
fun App() {
    val engine = rememberWebCoqEngine()

    CoqTheme {
        Box(Modifier.fillMaxSize()) {
            CoqEngineHost(engine = engine)
            ProofEditor(engine = engine)
        }
    }
}
