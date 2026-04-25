# Network decoupling — open follow-ups

Tracked work that fell out of the pylisa <-> lisa-network decoupling
(commits `5f42c26`, `9d7b7df`, `f0e9548` in pylisa; `b6d4c33`, `8d84d34`
in lisa-network). The decoupling itself is done and verified end-to-end:
pylisa builds and tests green standalone with zero network code or
deps; lisa-network depends on pylisa and owns everything network.

Two pieces of work are deliberately deferred:

---

## 1. Rewire the `eval/` pipeline to drive lisa-network instead of pylisa

**Where it lives today:** `pylisa/eval/` (gitignored — local working
data only, ~5 GB of cloned repos + analysis output).

**Files:** `eval/02_run_pylisa.py`, `eval/run_evaluation.sh`,
`eval/scripts/summarize_results.py`, plus `eval/repos_info.csv` and the
big untracked `eval/repos/` + `eval/analysis_output/`.

**Why it's broken now:** the script shells out to a `pylisa_bin` that
points at pylisa's gradle install output (`build/install/pylisa/bin/
pylisa`). That binary doesn't exist anymore — pylisa no longer has the
`application` plugin or a Main, because microservices analysis got
moved to lisa-network. The new entrypoint is
`lisa-network/build/install/lisa-network/bin/lisa-network`.

**What needs to change:**
- `02_run_pylisa.py`: update the `pylisa_bin` path constant (and ideally
  rename the variable + script). The CLI flags (`--main-file`,
  `--project-dir`, `--output-dir`) are unchanged because the new Main
  in lisa-network preserves the same signature.
- `run_evaluation.sh`: update the gradle build step to build
  lisa-network (`cd .../lisa-network/lisa-network && ./gradlew
  installDist`) instead of pylisa.
- Likely also relocate the entire `eval/` directory into lisa-network,
  since that's where its target binary lives now. lisa-network's
  `.gitignore` already has a placeholder `eval/` entry for this.
- The `summarize_results.py` script reads `final-network.txt` —
  unchanged, since the file format and the producing class
  (`FinalNetworkTxtResults`) didn't change, just its package.
- Re-run a small slice (~5 repos) to confirm end-to-end.

**Estimated effort:** ~1 hour. Mostly path edits + one verification
run.

---

## 2. Add a pylisa-side `imports4` test verifying degraded TOP fallback

**Why it doesn't exist yet:** the original `Imports.imports4` in pylisa
asserted that a multi-file `from routes import miningcore` chain
resolves `miningcore.router` to `fastapi.APIRouter*`. That assertion
needs `fastapi.txt` + the FastAPI Java backends on the classpath, both
of which moved to lisa-network. So the full-resolution test moved with
them and now lives at
`it.unive.lisa.microservices.imports.ImportsTest.imports4`.

**What's missing:** a complementary pylisa-side test that asserts the
*degraded* mode — i.e., what happens when fastapi is **not** on the
classpath (the situation pylisa-as-pure-Python-frontend always faces).

**Expected behavior in pylisa standalone:**
- `from routes import miningcore` falls back to open call
  (Unknown Module / "library not found")
- `y = miningcore.router` resolves to TOP / `#TOP#` (no info)
- `z = 3` still resolves correctly to constant `"3"`
- Builtins (`__new__`, `__init__`, `super`) are still registered

This verifies that the type system stays *sound* under the missing-
library fallback — i.e., we don't crash, don't silently invent a type,
and we preserve information about parts of the program that don't
depend on the missing library.

**Where to put it:** `pylisa/src/test/java/it/unive/pylisa/imports/
Imports.java` (the placeholder comment block already explains the
follow-up). The `py-testcases/imports/import4` directory itself moved
to lisa-network; the pylisa-side test will need a smaller stub
(probably 3 files: `api.py`, `routes/miningcore.py`, and a no-op
`__init__.py`) checked into pylisa under a new `import4_minimal/` or
similar.

**Estimated effort:** ~30 min. Mostly composing the right minimal
test fixture.

---

## Cross-reference

The original 5-PR plan from this work is at
`~/.claude/plans/i-was-thinking-to-steady-pelican.md` (consult for
context — including the future direction PR 6 about a YAML-based
network-spec loader that lisa-network would own).
