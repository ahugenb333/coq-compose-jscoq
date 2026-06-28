# Coq Compose — Kotlin Multiplatform

Interactive theorem proving on **Android and iOS** using Compose Multiplatform and a headless [compose-webview-multiplatform](https://github.com/KevinnZou/compose-webview-multiplatform) engine (jsCoq-ready, mock engine included).

## Structure

```
/androidApp              Android application shell
/iosApp                  Xcode project (wraps Shared framework)
/shared                  App entry — hosts WebView + proof editor
/core/models             Domain types and default Coq document
/core/engine             WebCoqEngine, CoqEngineHost, JS bridge assets
/feature/proof           ProofEditor screen
/ui                      Shared Material3 theme
```

## Run — Android

Open in Android Studio and run **`:androidApp`**.

```powershell
.\gradlew.bat :androidApp:installDebug
```

## Run — iOS

1. Set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.
2. Open `iosApp/iosApp.xcodeproj` in Xcode.
3. Build & run (Gradle embeds `:shared` via `embedAndSignAppleFrameworkForXcode`).

## What you get

The editor opens with:

```coq
Inductive nat : Set :=
| O : nat
| S : nat -> nat.
```

Type freely in the monospace field; the goal panel updates as you edit (debounced). Without jsCoq WASM assets, `core/engine/src/commonMain/resources/assets/js/bridge.js` provides a lightweight mock.

## Real jsCoq

1. Copy jsCoq into `core/engine/src/commonMain/resources/assets/jscoq/`.
2. Uncomment the loader in `index.html`.
3. Implement the `REAL` adapter in `bridge.js`.

Assets live in **`commonMain/resources/assets`** so Android and iOS load the same bundle.

## Architecture

```
ProofEditor (Compose, commonMain)
    ↓ CoqEngine interface
WebCoqEngine (commonMain)
    ↓ evaluateJavaScript("window.dispatchCoq(...)")
CoqEngineHost → WebView (Android WebView / iOS WKWebView)
    ↓ kmpJsBridge.callNative("CoqMessage", ...)
bridge.js → mock or jsCoq
```

The WebView is 1×1 dp and sits behind the editor — same headless pattern as the original Android-only prototype, now cross-platform.

## Licensing

Coq/Rocq is LGPL-2.1. Shipping unmodified binaries is fine; publish changes if you modify jsCoq/Coq itself.
