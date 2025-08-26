package dev.cohere.coq.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cohere.coq.engine.CoqEngine
import dev.cohere.coq.model.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProofApp(engine: CoqEngine, initialUri: String? = "file:///Main.v", initialText: String? = "Lemma t: forall n:nat, n + n = 2*n.\nProof.\n", autoProveProfile: String? = null) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(initialText ?: "Lemma t: forall n:nat, n + n = 2*n.\nProof.\n") }
    var suggest by remember { mutableStateOf<SuggestState?>(null) }
    var result by remember { mutableStateOf<ProveResult?>(null) }

    LaunchedEffect(Unit) {
        engine.init()
        engine.open(initialUri ?: "file:///Main.v", text)
        suggest = engine.peek(text.length)
        if (autoProveProfile != null) {
            result = engine.prove(initialUri ?: "file:///Main.v", autoProveProfile, text)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Coq Compose") })
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Editor", style = MaterialTheme.typography.labelLarge)
                    BasicTextField(
                        value = text,
                        onValueChange = { new ->
                            text = new
                            scope.launch {
                                engine.open("file:///Main.v", text) // simple: re-open; //TODO incremental
                                suggest = engine.peek(text.length)
                                result = null
                            }
                        },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            suggest?.let { s ->
                Text("Goal: ${s.goal.ty}", style = MaterialTheme.typography.bodyMedium)
                FlowRow(mainAxisSpacing = 8.dp, crossAxisSpacing = 8.dp) {
                    s.tactics.take(6).forEach { sugg ->
                        AssistChip(onClick = { text += sugg.t + ".\n" },
                            label = { Text(sugg.t) })
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    scope.launch { result = engine.prove("file:///Main.v", "fast", text) }
                }) { Text("Do proof") }
                if (result?.ok == true) {
                    Button(onClick = { text += (result?.script ?: "") + "\nQed.\n" }) { Text("Apply") }
                }
                OutlinedButton(onClick = { result = null }) { Text("Clear") }
                Button(onClick = {
                    scope.launch {
                        val sf = engine.javaClass.classLoader?.getResourceAsStream("assets/Basics.v")
                        text = """(* Software Foundations: Logical Foundations (Basics chapter, excerpt) *)
From Coq Require Import Arith.

Inductive day : Type :=
  | monday : day
  | tuesday : day
  | wednesday : day
  | thursday : day
  | friday : day
  | saturday : day
  | sunday : day.

Definition next_weekday (d:day) : day :=
  match d with
  | monday => tuesday
  | tuesday => wednesday
  | wednesday => thursday
  | thursday => friday
  | friday => monday
  | saturday => monday
  | sunday => monday
  end.

Example test_next_weekday:
  (next_weekday (next_weekday saturday)) = tuesday.
Proof. simpl. reflexivity. Qed.
"""
                        engine.open(initialUri ?: "file:///Basics.v", text)
                        suggest = engine.peek(text.length)
                        result = null
                    }
                }) { Text("Load Chapter 1") }
            }

            result?.let {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (it.ok) "✔ Proof found" else "✖ Partial", style = MaterialTheme.typography.titleSmall)
                        if (it.script != null) Text("Script: ${it.script}")
                        Text("Confidence: ${(it.confidence * 100).toInt()}%")
                    }
                }
            }

            Text("Drop jsCoq in assets/jscoq and enable loader in index.html to switch from mock → real.", style = MaterialTheme.typography.labelSmall)
        }
    }
}
