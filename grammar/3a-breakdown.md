# COS341 Phase 3a: Code Generation for ALGO — Work Breakdown

## 1. Scope

Translate the **main program's ALGO** into C code, "in the spirit of" Textbook Chapter 6.

**In scope (3a):** `ALGO`, `INSTR` (print, nop, comment, ASSIGN, BRANCH, LOOP), `TERM` (NAME, NUM, mod/div/add/sub/mul, neg), `BOOL` (eq, larger, lesser, not, or, and).

**Deferred to 3b (leave a stub marked `// 3b`):**
- `SPL_PROG`, `P`, `V_DECL`, `F_DECL`, `F_TYPE`
- `CALL` as an `INSTR` and as a `TERM` (inlining)

Note: `trans(ALGO)` must work on *any* ALGO subtree, not just the main one. In 3b the function bodies' ALGOs will be translated with the same method when calls are inlined.

## 2. Target-code rules (from the spec)

- No `else` in the target. Booleans become cascading `goto` jumps.
- `trans(BOOL, targetLabel, dummyLabel)` jumps to `targetLabel` when true and **falls through** otherwise.
- After every `;` write a newline (ASCII13).
- Operator mapping: `mod %`, `div /`, `add +`, `sub -`, `mul *`, `neg -`, `eq ==`, `larger >`, `lesser <`.
- Temporary variable type is `int` or `float`, as found by the type analyser (2b).
- `while` and `do-until` are translated via `not`, reusing the `until` and `do-while` translations.

## 3. Shared foundation (agree on this BEFORE parallel coding)

### 3.1 `CodeGenContext`

One object, passed to every translator.

```java
public class CodeGenContext {
    private int tempCounter = 0;
    private int labelCounter = 0;
    private final List<String> tempVars = new ArrayList<>();
    private final String cType;          // "int" or "float", from 2b
    private final SymbolTable symT;      // from 2a

    public String newVar()   { String n = "t" + (++tempCounter); tempVars.add(n); return n; }
    public String newLabel() { return "L" + (++labelCounter); }
    public String lookUp(Node nameNode) { ... }   // NAME -> internal name
    public String declarations() { ... }          // "int t1; int t2; ..."
}
```

Decisions:
- Temporaries use a prefix that cannot clash with 2a internal names (`t1`, not `v1`).
- Labels have their own counter and are never reused (this is what makes inlining safe in 3b).
- One `cType` for the whole program.

### 3.2 Method contracts

```java
String transAlgo (Node algo,  CodeGenContext ctx);
String transInstr(Node instr, CodeGenContext ctx);
String transTerm (Node term,  String resultVar, CodeGenContext ctx);
String transBool (Node bool,  String targetLabel, String dummyLabel, CodeGenContext ctx);
```

Rules:
- Every method returns a `String` of complete C code for that node.
- Statements end with `;` and a newline. Decide this once and stick to it.
- `transTerm` writes its result into `resultVar` and never returns a value.
- `transBool` jumps to `targetLabel` if true, otherwise falls through. `dummyLabel` is only passed along (swapped by `not` and `and`).
- Dispatch on the node's grammar rule name, not on child position. Remember keyword and punctuation tokens are leaf children in the tree.

### 3.3 Variable declarations: hoisted

- Translators only call `ctx.newVar()`; nobody writes `typ vN;` inline.
- `Main` writes `ctx.declarations()` once at the top, followed by the generated body.
- Guarantees no duplicates and no missing declarations.
- **Check first:** if the lecturer or marker expects the declaration written at the point of creation (as in the spec pseudocode), switch to the inline variant instead.
- User variables (`V_DECL`) are 3b. For testing, declare them by hand in a harness.

## 4. Split of work

### Person A: `TermGen` (TERM)

- `NAME` → `resultVar = intern`
- `NUM` → `resultVar = NUM`
- `add, sub, mul, div, mod` → fresh vars for both operands, then `result = x op y`
- `neg` → `result = -x`
- `CALL` → stub (3b)

### Person B: `BoolGen` (BOOL)

- `eq, larger, lesser` → `if ( x op y ) goto label`
- `not` → swap `targetLabel` and `dummyLabel`
- `or` → two sub-booleans in sequence, same labels
- `and` → `not(B1)` and `not(B2)` with labels swapped
- Depends on A's `transTerm`: stub it until A is done.
- Hand-trace label examples as tests (nested `not`, `and` inside `or`).

### Person C: `AlgoGen` / `InstrGen` (ALGO, INSTR) plus wiring

- `ALGO` (ε → `/* end */`; `INSTR ; ALGO`) with newline after each `;`
- `print` (STRING variant and `( TERM )` variant), `nop`, `comment`, `ASSIGN`
- `BRANCH`: `La, Lb, Lc`, false code first, `goto Lc`, then `La:` true code, then `Lc:`
- `LOOP`: until, while, do-while, do-until (while and do-until via `not`)
- `CALL` → stub (3b)
- Output file writing and `Main` wiring (declarations first, then body)
- Depends on `transBool`: stub it while B is working.

Note: C has the biggest share but the most mechanical work. If needed, move `ASSIGN` and `print` to whoever finishes first.

## 5. Order of work

1. **All:** agree on `CodeGenContext`, signatures and the declaration choice (about 30 min).
2. **A and B in parallel**, B stubbing `transTerm`; **C** writes the structural cases against a stub `transBool`.
3. **Integrate**, then run small SPL programs end to end and compile the generated C to check it.

## 6. Watch-outs

- `not` swaps the labels: easy to flip twice by accident.
- `BRANCH` label order: false code sits directly behind the decision code (after `Lb:`), true code sits behind `La:`.
- Labels in C use a colon `:`; do not confuse with the semicolon.
- `until` loop: `ls:`, decision code, `d:`, body, `goto ls;`, `le:`.
- `do-while` loop: `ld:`, body, decision code jumping to `ld` (dummy is `le`), `le:`.
- Never reuse a label or temp name.
- Keep stubs for 3b clearly marked so they are easy to find.

## 7. Definition of done for 3a

- [ ] `CodeGenContext` and signatures agreed and committed
- [ ] `TermGen` complete and tested
- [ ] `BoolGen` complete and tested
- [ ] `AlgoGen` / `InstrGen` complete (including all four loops and BRANCH)
- [ ] `Main` writes declarations then body to the output file
- [ ] End-to-end test: generated C compiles and prints expected output
- [ ] README section listing the new files, interfaces and call order
