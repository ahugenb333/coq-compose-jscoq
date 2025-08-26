package dev.cohere.coq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import dev.cohere.coq.ui.ProofApp
import dev.cohere.coq.engine.CoqEngine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val engine = CoqEngine(applicationContext) // retained across recompositions
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                AppRoot(engine, startRoute = intent.asStartRoute())
            }
        }
    }
}
