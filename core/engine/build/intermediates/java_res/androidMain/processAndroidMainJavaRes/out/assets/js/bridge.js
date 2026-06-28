// Headless jsCoq bridge for Compose Multiplatform WebView (Android + iOS).
//
// Native → JS:  navigator.evaluateJavaScript("window.dispatchCoq({...})")
// JS → Native:  window.kmpJsBridge.callNative("CoqMessage", JSON.stringify({id,type,payload}), null)

(function() {
  function postToNative(obj) {
    const payload = JSON.stringify(obj);
    const attempt = () => {
      if (window.kmpJsBridge && window.kmpJsBridge.callNative) {
        window.kmpJsBridge.callNative("CoqMessage", payload, null);
      } else {
        setTimeout(attempt, 50);
      }
    };
    attempt();
  }

  function respond(id, payload) {
    postToNative({ id: id, type: "response", payload: payload });
  }

  function sendReady() {
    postToNative({ id: -1, type: "ready", payload: { ok: true } });
  }

  const DEFINITIONS_GOAL = {
    ty: "Definitions loaded — add a Lemma … Proof. to start proving",
    ctx: [],
  };

  const mockState = { text: "", goal: DEFINITIONS_GOAL, solved: false };

  function parseGoal(text) {
    const proofBlock = text.match(/Proof\.\s*([\s\S]*)$/i);
    if (proofBlock && !/Qed\./.test(proofBlock[1])) {
      const lemma = text.match(/Lemma\s+\w+\s*:\s*(.*?)\./s);
      if (lemma) return { ty: lemma[1].trim(), ctx: [] };
    }
    const lemmaOnly = text.match(/Lemma\s+\w+\s*:\s*(.*?)\./s);
    if (lemmaOnly && !/Proof\./.test(text)) {
      return { ty: lemmaOnly[1].trim(), ctx: [] };
    }
    if (/Inductive\s+\w+/i.test(text) || /Definition\s+\w+/i.test(text)) {
      return DEFINITIONS_GOAL;
    }
    return DEFINITIONS_GOAL;
  }

  function mockHeuristics(goal) {
    const g = (goal.ty || "").toLowerCase();
    const t = [];
    if (g.includes("definitions loaded")) return t;
    if (g.includes("forall")) t.push({ t: "intros", score: 0.85 });
    if (g.includes("=")) t.push({ t: "simpl", score: 0.6 });
    if (g.includes("nat")) t.push({ t: "lia", score: 0.62 });
    t.push({ t: "reflexivity", score: 0.55 });
    t.push({ t: "auto", score: 0.4 });
    return t;
  }

  const MOCK = {
    open: (uri, text) => {
      mockState.text = text || "";
      mockState.goal = parseGoal(mockState.text);
      mockState.solved = /Qed\./.test(mockState.text);
      return { ok: true };
    },
    peek: (cursor) => ({
      goal: mockState.solved ? { ty: "done", ctx: [] } : mockState.goal,
      tactics: mockHeuristics(mockState.goal),
    }),
    exec: (tactic) => {
      const tac = (tactic || "").toLowerCase().trim();
      if (
        (tac.startsWith("lia") && mockState.goal.ty.includes("nat")) ||
        (tac.startsWith("ring") && mockState.goal.ty.includes("=")) ||
        tac.startsWith("reflexivity") ||
        tac.startsWith("exact") ||
        tac.startsWith("easy")
      ) {
        mockState.solved = true;
        mockState.goal = { ty: "done", ctx: [] };
      }
      return { goal: mockState.goal };
    },
    prove: (uri, text, profile) => {
      MOCK.open(uri, text);
      const order = profile === "thorough"
        ? ["intros", "simpl", "ring", "lia", "auto"]
        : ["intros", "simpl", "lia", "auto"];
      const script = [];
      for (const t of order) {
        MOCK.exec(t);
        script.push(t + ".");
        if (mockState.solved) break;
      }
      return {
        ok: mockState.solved,
        script: script.join(" "),
        alt: ["auto.", "easy."],
        confidence: mockState.solved ? 0.75 : 0.3,
      };
    },
  };

  const REAL = (function() {
    let ready = false;
    let currentGoal = { ty: "?", ctx: [] };

    async function boot() {
      if (ready) return;
      // TODO: session = await jsCoq.boot({ packages: ["coq","init"], all_pkgs: true });
      ready = true;
    }

    return {
      open: async (uri, text) => { await boot(); return { ok: true }; },
      peek: async (cursor) => ({ goal: currentGoal, tactics: MOCK.peek(cursor).tactics }),
      exec: async (tactic) => ({ goal: currentGoal }),
      prove: async (uri, text, profile) => {
        await REAL.open(uri, text);
        return { ok: false, script: "", alt: ["auto.", "easy."], confidence: 0.4 };
      },
    };
  })();

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

  document.addEventListener("DOMContentLoaded", sendReady);
})();
