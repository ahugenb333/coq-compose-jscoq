# Place jsCoq build here

Copy the jsCoq distribution into this folder, e.g.:

- `jscoq/` (directory)
  - `coq.worker.js`
  - `jscoq_worker.bc.js`
  - `coq-pkg.js`
  - `coq-wasm.wasm`
  - `coq-stdlib/` ...

Then add `<script src="jscoq/jscoq_loader.js"></script>` to `assets/index.html` (already has a TODO).
The bridge will call into jsCoq when integrated (see `assets/js/bridge.js` //TODO sections).
