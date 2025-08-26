// Headless bridge for jsCoq in Android WebView.
//
// Modes:
//   1) REAL: if jsCoq loader is present (window.jsCoq / window.coq) → use real engine
//   2) MOCK: lightweight heuristics so the app runs without assets
//
// Contract: Android calls window.dispatchCoq({id, type, ...}).
// We respond via Android.onMessage(JSON.stringify({id, type:'response', payload})).

(function() {
  // ---- helpers ---------------------------------------------------------------
  function respond(id, payload) {
    const msg = JSON.stringify({ id: id, type: "response", payload: payload });
    if (window.Android && Android.onMessage) Android.onMessage(msg);
    else console.log("Android bridge missing; msg:", msg);
  }

  function sendReady() {
    if (window.Android && Android.onMessage) {
      Android.onMessage(JSON.stringify({ id: -1, type: "ready", payload: { ok: true } }));
    } else {
      console.log("Bridge ready (no Android)");
    }
  }

  // ---- MOCK engine -----------------------------------------------------------
  const mockState = { text: "", goal: { ty: "forall n:nat, n+n = 2*n", ctx: ["n:nat"] }, solved: false };
  function mockHeuristics(goal) {
    const g = (goal.ty || "").toLowerCase();
    const t = [];
    if (g.includes("forall")) t.push({ t: "intros", score: 0.85 });
    if (g.includes("=")) t.push({ t: "simpl", score: 0.6 });
    if (g.includes("nat")) t.push({ t: "lia", score: 0.62 });
    t.push({ t: "auto", score: 0.4 });
    t.push({ t: "eauto", score: 0.35 });
    return t;
  }
  const MOCK = {
    open: (uri, text) => {
      mockState.text = text || "";
      const m = mockState.text.match(/Lemma\s+\w+\s*:\s*(.*)\./);
      mockState.goal.ty = m ? m[1].trim() : mockState.goal.ty;
      mockState.solved = /Qed\./.test(mockState.text);
      return { ok: true };
    },
    peek: (cursor) => ({ goal: mockState.goal, tactics: mockHeuristics(mockState.goal) }),
    exec: (tactic) => {
      const tac = (tactic || "").toLowerCase().trim();
      if ((tac.startsWith("lia") && mockState.goal.ty.includes("nat")) ||
          (tac.startsWith("ring") && mockState.goal.ty.includes("=")) ||
          tac.startsWith("exact") || tac.startsWith("easy") ) {
        mockState.solved = true;
        mockState.goal = { ty: "⊢ ☐", ctx: [] };
      }
      return { goal: mockState.goal };
    },
    prove: (uri, text, profile) => {
      MOCK.open(uri, text);
      const order = profile === "thorough"
        ? ["intros","simpl","ring","lia","auto"]
        : ["intros","simpl","lia","auto"];
      const script = [];
      for (const t of order) {
        MOCK.exec(t);
        script.push(t + ".");
        if (mockState.solved) break;
      }
      return { ok: mockState.solved, script: script.join(" "), alt: ["auto.", "easy."], confidence: mockState.solved ? 0.75 : 0.3 };
    }
  };

  // ---- REAL jsCoq adapter (scaffold) ----------------------------------------
  // This assumes jsCoq loader exposes a global 'jsCoq' or 'coq' API.
  // Fill in the TODOs based on your jsCoq build version.
  const REAL = (function() {
    let ready = false;
    let session = null;
    let currentGoal = { ty: "⊢ ?", ctx: [] };

    async function boot() {
      if (ready) return;
      // TODO: initialize jsCoq with desired packages.
      // Example (pseudocode; adjust to your jsCoq):
      // session = await jsCoq.boot({ packages: ["coq","init"], all_pkgs: true });
      // ready = true;
      ready = true; // remove when real boot is wired
    }

    async function addText(text) {
      // TODO: feed Coq document up to a point.
      // Pseudocode: await session.add(text);
      return true;
    }

    async function getGoals() {
      // TODO: query goals from jsCoq, map to {ty, ctx}
      // Pseudocode:
      // const g = await session.goals();
      // currentGoal = translate(g);
      return currentGoal;
    }

    async function runTactic(tac) {
      // TODO: send a tactic to Coq (e.g., 'by lia.').
      // Pseudocode: await session.exec(tac);
      // Update currentGoal based on new goals:
      // currentGoal = await getGoals();
      return currentGoal;
    }

    return {
      open: async (uri, text) => { await boot(); await addText(text || ""); return { ok: true }; },
      peek: async (cursor) => { const g = await getGoals(); return { goal: g, tactics: MOCK.peek(cursor).tactics }; },
      exec: async (tactic) => { const g = await runTactic(tactic); return { goal: g }; },
      prove: async (uri, text, profile) => {
        await REAL.open(uri, text);
        const order = profile === "thorough" ? ["intros","simpl","ring","lia","auto"] : ["intros","simpl","lia","auto"];
        const script = [];
        for (const t of order) {
          await REAL.exec(t);
          script.push(t + ".");
          // TODO: detect solved via real goals
          if (false) break; // replace with 'if (solved)'
        }
        return { ok: false /* TODO set real */, script: script.join(" "), alt: ["auto.","easy."], confidence: 0.4 };
      }
    };
  })();

  // ---- dispatcher ------------------------------------------------------------
  const usingReal = !!(window.jsCoq || window.coq);
  const engine = usingReal ? REAL : MOCK;

  window.dispatchCoq = function(cmd) {
    const id = cmd && cmd.id || 0;
    const t = cmd && cmd.type;
    const run = async () => {
      try {
        switch (t) {
          case "open": return await engine.open(cmd.uri, cmd.text);
          case "peek": return await engine.peek(cmd.cursor);
          case "exec": return await engine.exec(cmd.text);
          case "prove": return await engine.prove(cmd.uri, cmd.text, cmd.profile);
          default: return { error: "unknown cmd: " + t };
        }
      } catch (e) {
        return { error: String(e) };
      }
    };
    Promise.resolve(run()).then(payload => respond(id, payload));
  };

  // Signal ready to Android
  document.addEventListener("DOMContentLoaded", sendReady);
})();
