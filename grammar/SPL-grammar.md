# SPL Context-Free Grammar (COS341 2026)

Source of truth: `Prac-Spec-Syntax.pdf`. This file tracks our LL(1) analysis
and any grammar transformations, so the parser implementation and this doc
never drift apart.

## Original grammar

See the PDF for the full production list.

## LL(1) suitability

**Not LL(1) as given.** Two conflicts, both caused by `CALL`:

```
TERM  → USER-DEFINED-NAME | CALL      (CALL → USER-DEFINED-NAME ( INPUT ))
INSTR → ASSIGN | CALL                 (ASSIGN → USER-DEFINED-NAME = TERM)
```

In both cases, a single lookahead token (`USER-DEFINED-NAME`) doesn't
distinguish the alternatives — you need to see the *second* token (`=` vs
`(` vs neither) to know which production applies.

Everything else in the grammar checked out clean: `V_DECL`/`F_DECL` are
simple nullable right-recursive lists, `ALGO`'s two alternatives don't
collide (FOLLOW(ALGO) = `{ }, $, return }` is disjoint from FIRST(INSTR)),
`LOOP`'s two alternatives start with disjoint keyword sets (`while`/`until`
vs `do`), and `BOOL`'s six alternatives are all distinct keywords.

## Decision

- [ ] Left-factor `TERM` and `INSTR` and hand-write LL(1) recursive descent, OR
- [ ] Build SLR/LALR(1) tables on the grammar as-is

**Status:** TODO — decide as a group and check this box + fill in below.

### If left-factoring (Option A)

```
TERM   → USER-DEFINED-NAME TERM' | NUM | mod(TERM TERM) | add(TERM TERM)
        | sub(TERM TERM) | mul(TERM TERM) | div(TERM TERM) | neg(TERM)
TERM'  → ( INPUT ) | ε

INSTR  → print OUTP | nop | comment STRING | USER-DEFINED-NAME INSTR'
        | BRANCH | LOOP
INSTR' → = TERM | ( INPUT )
```
Verified: FOLLOW(TERM) does not contain `(`, so `TERM' → ε` vs
`TERM' → ( INPUT )` do not collide. Tag the `( INPUT )` branches as CALL
nodes when building the tree, since `CALL` is no longer a separate
production.

### If SLR/LALR (Option B)

Grammar stays as-is; build canonical LR(0)/SLR item sets. Document the
states + ACTION/GOTO table here (or link to a generated file) once built.

## Lexer notes

- Every token must be followed by a blank_space (ASCII 32 or 13) — see PDF.
- `USER-DEFINED-NAME` always starts with `#`, so it can never collide with a
  keyword lexically — no maximal-munch-then-keyword-lookup needed, unlike a
  typical lexer.
