# Flask methods= probe — heap-based list lowering experiment (reverted)

These are working-tree-only versions of four pylisa files that were rewritten
so `lisa-network/.../flask/Route.java::resolveMethods` could probe a Python
`methods=[...]` list literal cell-by-cell via `AccessChild`/`HeapDereference`.

They were reverted on **2026-04-25** because they broke convergence of the
`testNetflixDispatch` evaluation: dispatch has many list literals passed to
constructors (`[SensitiveProjectActionPermission]`, `dependencies=[...]`,
`JSON default=list()`, …) and the new heap-based lowering caused the
inter-procedural fixpoint to keep producing `INCOMPARABLE` results on
`PermissionsDependency.__init__::$call` (~94 iterations and growing).

## Files

| File | What changed vs HEAD |
|---|---|
| `ListCreation.java` | `MemoryAllocation` + per-cell `AccessChild(deref, idx)` writes + `size` cell |
| `SequenceGetItem.java` | `AccessChild(deref(seq), index)` instead of `PushAny` |
| `SequenceLen.java` | reads `size` cell from heap instead of `PushAny` |
| `ConstantPropagation.java` | adds `ComparisonEq/Ne` eval + `satisfiesBinaryExpression` (needed by `Route`'s `Analysis.satisfies` probe) |

## When to revisit

Flask `methods=[...]` resolution still works without these — `Route` falls
back to GET-only. If we later want precise methods, the better design is:

1. Keep `ListCreation` value-level (current HEAD).
2. In `Route.resolveMethods`, walk the `params[idx]` `ExpressionSet` directly
   for `BinaryExpression(ListConstant, …, ListAppend)` chains rather than
   reading from the heap. That keeps the universal list lowering untouched
   and avoids the convergence cost on every list literal in the program.

## Companion changes (still active)

- `lisa-network/.../flask/Route.java` — adds `methods=` kwarg + `resolveMethods()` (will fall back to GET-only without these companion changes)
- `lisa-network/.../resources/libraries/flask.txt` — adds `&methods` parameter binding
