package dev.cohere.coq.feature.proof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cohere.coq.engine.CoqEngine
import dev.cohere.coq.model.DEFAULT_COQ_TEXT
import dev.cohere.coq.model.DEFAULT_COQ_URI
import dev.cohere.coq.model.SuggestState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProofEditor(
    engine: CoqEngine,
    initialText: String = DEFAULT_COQ_TEXT,
    documentUri: String = DEFAULT_COQ_URI,
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText) }
    var suggest by remember { mutableStateOf<SuggestState?>(null) }
    var status by remember { mutableStateOf("Starting engine…") }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    suspend fun refreshDocument(newText: String) {
        engine.open(documentUri, newText)
        suggest = engine.peek(newText.length)
        status = "Ready"
    }

    LaunchedEffect(engine) {
        engine.init()
        refreshDocument(text)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Coq") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            suggest?.let { state ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("Goal", style = MaterialTheme.typography.labelLarge)
                        Text(state.goal.ty, style = MaterialTheme.typography.bodyMedium)
                        if (state.goal.ctx.isNotEmpty()) {
                            Text(
                                state.goal.ctx.joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            Card(modifier = Modifier.weight(1f)) {
                BasicTextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        status = "Updating…"
                        debounceJob?.cancel()
                        debounceJob = scope.launch {
                            delay(350)
                            refreshDocument(newText)
                        }
                    },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }

            Text(status, style = MaterialTheme.typography.labelSmall)
        }
    }
}
