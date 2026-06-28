# jsCoq assets

Copy a jsCoq distribution here to run real Coq in the WebView:

| File / directory | Role |
|---|---|
| `jscoq_loader.js` | Entry loader (uncomment in `index.html`) |
| `coq.worker.js` | Coq worker |
| `jscoq_worker.bc.js` | Emscripten worker |
| `coq-pkg.js` | Package loader |
| `coq-wasm.wasm` | Coq compiled to WebAssembly |
| `coq-stdlib/` | Standard library |

Then implement the `REAL` adapter TODOs in `js/bridge.js`.
