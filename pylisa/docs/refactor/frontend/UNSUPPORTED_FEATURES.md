# Python Features Not Yet Supported by the PyLiSA Front-end

Inventory of Python grammar productions that the front-end currently rejects.
The source of truth is
[`UnsupportedGrammarFeatures.FEATURES`](../../../src/main/java/it/unive/pylisa/frontend/UnsupportedGrammarFeatures.java);
every rejection flows through that registry, emits an `UNSUPPORTED`
`DiagnosticReporter` event, and throws `UnsupportedStatementException`.

To enable a feature, remove its entry from the registry, implement the
corresponding `visit*` method in the appropriate category visitor, and add a
unit test.

---

## 1 · Registered grammar features (rejected at visit time)

These rule-context classes are in the current Python 3.3 ANTLR grammar used by
the front-end, but the corresponding visit method is a one-line stub that
routes to `support.rejectUnsupported(ctx)`.

| ANTLR rule context | User-facing label | Notes |
|---|---|---|
| `Yield_exprContext` | yield expression | inside an expression (not a statement) |
| `Yield_argContext` | yield argument | the RHS of `yield from …` / `yield x` |
| `Yield_stmtContext` | yield statement | bare `yield` as a statement |
| `Star_exprContext` | star expression | `*a` in unpacking targets |
| `Encoding_declContext` | encoding declaration | encoding cookie; unreachable from normal source |
| `TrailerContext` | trailer (in unsupported position) | trailer seen outside the dispatch in `visitAtom_expr` |
| `SubscriptlistContext` | subscript list | `a[i, j]` multi-slice |
| `SliceopContext` | slice step | `a[::k]` |
| `Comp_forContext` | for-clause of comprehension | `[x for x in …]` generator/comprehension clause |
| `Comp_ifContext` | if-clause of comprehension | `[x for x in … if …]` |
| `Comp_iterContext` | comprehension iterator | inner recursive rule of comprehensions |
| `AnnassignContext` | annotated assignment (standalone) | `x: int` with no RHS inside a function body |
| `AugassignContext` | augmented assignment | `x += 1`, `x *= 2`, … |
| `Global_stmtContext` | global declaration | `global x` inside a function |
| `Nonlocal_stmtContext` | nonlocal declaration | `nonlocal x` inside a nested function |
| `Flow_stmtContext` | flow statement | dispatch fallthrough for unknown flow variant |
| `Raise_stmtContext` | raise statement | `raise X(...)` |
| `Async_stmtContext` | async statement | `async for …`, `async with …` (non-`def` forms) |
| `Except_clauseContext` | typed except clause | `except T: …` / `except T as e: …` |
| `Import_as_nameContext` | import-as name | `from m import x as y` |
| `Dotted_as_nameContext` | dotted import-as name | `import m as n` / `import a.b as c` |
| `Import_as_namesContext` | import-as list | grammar-internal list of `import-as` |
| `Dotted_as_namesContext` | dotted import-as list | grammar-internal list of `dotted-as` |
| `VarargslistContext` | untyped parameter list | untyped `lambda` parameter list |

## 2 · Feature-gap branches inside larger visit methods

These are `support.rejectUnsupported(ctx, "<label>")` calls inside visit
methods that otherwise have real logic; the label is hard-coded at the call
site rather than looked up via the FEATURES map.

| Label | Where | Why |
|---|---|---|
| `yield expression in atom` | `LiteralVisitor.visitAtom` | `(yield …)` as an atom |
| `yield expression in parens` | `LiteralVisitor.visitTupleOrParenthesized` | parenthesised yield |
| `complex literal` | `LiteralVisitor.visitNumberAtom` | numeric literals with `j`/`J` suffix |
| `dict comprehension` | `LiteralVisitor.visitDictorsetmaker` | `{k: v for …}` / non-empty COLON in dict-set maker |
| `generator comprehension` | `LiteralVisitor.visitTestlist_comp` | `(x for x in …)` |
| `star expression in testOrStar` | `LiteralVisitor.visitTestOrStar` | `*x` inside tuple/list element position |
| `atom variant` | `LiteralVisitor.visitAtom` | fallthrough for a future atom form |
| `multi-target expression statement` | `SimpleStatementVisitor.visitExpr_stmt` | `a, b` as statement with no assignment |
| `star expression in exprlist` | `StatementVisitor.visitExprlist` | starred elements in an exprlist |

## 3 · Grammar-level gaps (not in the current grammar at all)

The front-end uses the Bart-Kiers Python 3.3 grammar (`src/main/antlr/`).
These newer Python features are not representable in that grammar and
therefore fail at the ANTLR layer before the visitor ever runs:

- `:=` walrus operator (PEP 572, Python 3.8)
- `match` / `case` statements and patterns (PEP 634, Python 3.10)
- `X | Y` union type annotation (PEP 604, Python 3.10)
- positional-only parameters `/` (PEP 570, Python 3.8)
- `type X = int` type aliases (PEP 695, Python 3.12)
- `except*` exception groups (PEP 654, Python 3.11)
- Structural f-string internals (PEP 701, Python 3.12)

The tracking initiative for migrating to the Python 3.13 grammar is
[`frontend13-plan.md`](../../../frontend13-plan.md).

## 4 · Regenerating this document

The inventory in §1 mirrors the
`UnsupportedGrammarFeatures.FEATURES` map exactly. If an entry is added or
removed there, update this file. A future `./gradlew listUnsupportedFeatures`
task can dump the map to console to keep the two in sync automatically.
