# Coq Compose (Headless jsCoq + Kotlin/Compose)

A cohesive starter that bundles:
- **Android app with Jetpack Compose**: native UI (editor, tactic chips, "Do proof")
- **Headless jsCoq bridge in a hidden WebView**: on-device evaluation
- Strategy-ish behavior for "Do proof" (cheap portfolio)
- **WireMock** folder to stand up a "real fake server" that mimics an HTTP API (optional)

> Uses jsCoq if you drop assets; otherwise falls back to a lightweight mock so you can run immediately.

## Structure
```
/app
  src/main/java/dev/cohere/coq/...
  src/main/assets/index.html
  src/main/assets/js/bridge.js         # ↔ Android bridge; //TODO integrate real jsCoq calls
  src/main/assets/jscoq/README.md      # where to place jsCoq build
/wiremock                               # optional fake server
```

## Get running (mock fallback)
1) Open the project in Android Studio (Giraffe+), JDK 17.
2) Run the app. You'll see a Compose editor, goal preview, tactic chips, and "Do proof".
   - Without jsCoq assets, it still works using a mock evaluator in `bridge.js`.

## Use **real jsCoq**
1) Build/download jsCoq (or grab a release).
2) Copy the distribution into `app/src/main/assets/jscoq/` (see README.md in that folder).
3) Edit `app/src/main/assets/index.html` to include the loader script:
   ```html
   <script src="jscoq/jscoq_loader.js"></script>
   ```
4) In `assets/js/bridge.js`, replace the //TODO section to call the jsCoq API (e.g., add/exec/query).
   - The Android bridge expects `window.dispatchCoq({id,type, ...})` to `respond(id, payload)`.

## Minimal Compose "head"
- Editor (monospace)
- Suggest-as-you-type: naive peek + heuristic tactics (or real from jsCoq when wired)
- "Do proof" button: runs a cheap tactic portfolio (`intros; simpl; lia|auto`)

## WireMock (optional)
```
cd wiremock
docker compose up
# POST to localhost:8081/v1/sessions|suggest|prove
```

## Notes
- Licensing: Coq/Rocq is LGPL-2.1; shipping unmodified binaries is fine. If you modify jsCoq/Coq itself, you must publish those changes.
- This starter keeps the **engine boundary** JS-side so you can later swap in a remote API or a native engine.
