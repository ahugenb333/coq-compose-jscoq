package dev.cohere.coq.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.navigation.compose.*
import androidx.navigation.*
import dev.cohere.coq.engine.CoqEngine
import dev.cohere.coq.nav.Route

@Composable
fun AppRoot(engine: CoqEngine, startRoute: Route? = null) {
    val nav = rememberNavController()
    LaunchedEffect(startRoute) {
        when (val r = startRoute) {
            is Route.Open -> nav.navigate("open?uri=${r.uri ?: ""}&text=${r.text ?: ""}")
            is Route.Prove -> nav.navigate("prove?uri=${r.uri ?: ""}&text=${r.text ?: ""}&profile=${r.profile ?: "fast"}")
            null -> {}
        }
    }
    NavHost(navController = nav, startDestination = "editor") {
        composable("editor") { ProofApp(engine = engine) }
        composable(
            route = "open?uri={uri}&text={text}",
            arguments = listOf(
                navArgument("uri") { nullable = true; defaultValue = null },
                navArgument("text") { nullable = true; defaultValue = null }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "coq://app/open?uri={uri}&text={text}" },
                navDeepLink { uriPattern = "https://coq.example/open?uri={uri}&text={text}" }
            )
        ) { e ->
            val uri = e.arguments?.getString("uri")
            val text = e.arguments?.getString("text")
            ProofApp(engine = engine, initialUri = uri, initialText = text)
        }
        composable(
            route = "prove?uri={uri}&text={text}&profile={profile}",
            arguments = listOf(
                navArgument("uri") { nullable = true; defaultValue = null },
                navArgument("text") { nullable = true; defaultValue = null },
                navArgument("profile") { nullable = true; defaultValue = "fast" }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "coq://app/prove?uri={uri}&text={text}&profile={profile}" },
                navDeepLink { uriPattern = "https://coq.example/prove?uri={uri}&text={text}&profile={profile}" }
            )
        ) { e ->
            val uri = e.arguments?.getString("uri")
            val text = e.arguments?.getString("text")
            val profile = e.arguments?.getString("profile") ?: "fast"
            ProofApp(engine = engine, initialUri = uri, initialText = text, autoProveProfile = profile)
        }
    }
}
