
# COS341 Phase 3b: Functions and Calls via Inlining — Work Breakdown

Written against `main` @ `f94d45a` ("Done with Term"). Everything marked **(verified)** was read from the repo, not assumed.

## 0. Read this first: state of `main`

Four things in the repo affect the split. Settle them in the kickoff.

1. **`AlgoGen` on `main` is still all stubs, because the finished version has not been pushed yet.** All five methods (`transAlgo`, `transInstr`, `transAssign`, `transBranch`, `transLoop`) throw `UnsupportedOperationException("TODO")` on `main`. `TermGen` and `BoolGen` are done. 3b translates function bodies with `transAlgo`, and the `CALL` case lives in `transInstr`, so **it must be pushed before anyone can run 3b end to end.** Push it first (or share the file), and check that its `CALL` branch and signatures match section 4.3 before Person 2 writes the hook.
2. **`Main.java` never calls code generation.** It stops after the type analyser. `CodeGenMain.generate(Node mainAlgo, ...)` also takes only the main `ALGO`; 3b needs the whole `SPL_PROG` root.
3. **Nothing decides `int` vs `float`.** `TypeAnalyser.analyse` returns `Type.OK`, and `CodeGenContext` just takes a `cType` string that nobody computes yet.
4. **`CodeGenContext` has no knowledge of functions.** It needs a function registry (section 4).

## 1. Scope

**In scope (3b):** `SPL_PROG`, `P`, `V_DECL`, `F_DECL`, `F_TYPE`, `CALL` as `INSTR` and as `TERM`, `INPUT`. Also the executable wrapper (`#include`, `int main()`, `return 0;`), the commented-out code-mine, and the two hard cases from the sheet: nested inlining and unique labels per inlined copy.

**Already done in 3a, reused as is:** `TermGen`, `BoolGen`, `CodeGenContext.newLabel()/newVar()`, `GenKinds` helpers.

## 2. Key design decision: inline from the TREE, not from text

The sheet describes "copy and paste the code-mine". If the mine is stored as **strings**, every paste repeats the same labels (`L3:` twice), and you would have to find and rename them with regexes. That is fragile.

Instead, at each call site we **re-run `AlgoGen.transAlgo` on the callee's body subtree**. `ctx.newLabel()` never reuses a number, so each inlined copy gets fresh labels and fresh temps for free. The two hard cases then come out naturally:

- **Unique labels:** guaranteed by the global label counter (already a 3a decision).
- **Nested inlining (`f` calls its own `g`):** translating `f`'s body hits `CALL g`, which inlines `g` recursively. No separate "phase 1 inside the mine" is needed.

The code-mine is still written to the output as the sheet requires, as a **comment** generated once per function. It is documentation for the marker; the executable code does not read from it.

**Termination is guaranteed:** `FunctionChecker.resolveFunctionCall` calls `lookupFunc`, which searches only the *current* scope. A function's own name is not in scope inside its own body, so there is no recursion. Still add a cheap depth guard (e.g. throw at depth > 100) so a bug cannot hang the compiler.

## 3. Facts about the tree and symbol table (verified)

**Tree shapes** (from `ProgramParser`/`InstrParser`). Name leaves keep their `#` prefix, and punctuation and keywords are leaf children:

| Node              | Children                                                                                              |
| ----------------- | ----------------------------------------------------------------------------------------------------- |
| `SPL_PROG`      | `P`, `EOF` leaf                                                                                   |
| `P`             | `V_DECL`, `:`, `F_DECL`, `:`, `ALGO`                                                        |
| `V_DECL`        | empty, or `#name`, `V_DECL` (right-recursive)                                                     |
| `F_DECL`        | empty, or `F_TYPE`, `F_DECL`                                                                      |
| `F_TYPE` (void) | `void`, `#name`, `(`, `V_DECL`, `)`, `{`, `P`, `return`, `}`                        |
| `F_TYPE` (num)  | `num`, `#name`, `(`, `V_DECL`, `)`, `{`, `P`, `return`, `(`, `TERM`, `)`, `}` |
| `CALL`          | `#name`, `(`, `INPUT`, `)`                                                                    |
| `INPUT`         | empty, or `TERM`, `INPUT` (right-recursive)                                                       |
| `INSTR` (call)  | `CALL`                                                                                              |

**Symbol table:**

- `symT.getEntry(nodeId)` is public. For the `#name` leaf of a call it returns the callee's `FUNC` entry. This is recorded by `resolveFunctionCall`.
- `entry.getDeclNodeId()` is the node id of the **`#name` leaf inside the callee's `F_TYPE`**. So `registry` can be keyed by that id, and `fTypeNameLeaf.getParent()` is the `F_TYPE`.
- `entry.getUniqueName()` gives `f1, f2, ...` for functions and `v1, v2, ...` for variables **and parameters**. These are globally unique across the whole program, so one declaration per name at the top of `main` is enough, even though the body is inlined several times.
- Parameter and local-variable names are recorded per `#name` leaf in their `V_DECL`, so `ctx.lookUp(leaf)` already works for them.

## 4. Shared contracts (agree on these BEFORE parallel coding)

### 4.1 `CodeGenContext` additions (Lead)

```java
// keep the existing 2-arg constructor so CodeGenContextTest still compiles
public CodeGenContext(SymbolTable symT, String cType, FunctionRegistry registry)

public String newVar(String prefix);            // returns prefix + "t" + n, registered for hoisting (used for the return var r = f ++ n)
public void   declareUserVar(String uniqueName);// hoisted; LinkedHashSet so no duplicates
public FunctionRegistry registry();
public CodeGenContext scratch();                // fresh counters/lists, same symT/cType/registry; used to generate the code-mine
public String declarations();                   // user vars first, then temps
```

### 4.2 `FunctionRegistry` and `FunctionInfo` (skeleton by Lead, filled in by Person 2), new, package `spl.codegen`

```java
final class FunctionInfo {
    String uniqueName;          // f1, f2, ...
    boolean isVoid;
    List<Node> paramNameNodes;  // #name leaves of the F_TYPE's V_DECL, in order
    Node bodyP;                 // the F_TYPE's P node
    Node returnTerm;            // TERM child of F_TYPE, null for void
}
final class FunctionRegistry {
    static FunctionRegistry build(Node root);                      // crawl EVERY F_TYPE in the tree (nested too); key = id of its #name leaf
    FunctionInfo forCall(Node callNode, SymbolTable symT);         // callNameLeaf -> symT.getEntry(id).getDeclNodeId() -> info
}
```

### 4.3 `CallGen` (skeleton by Lead, filled in by Person 2), new

```java
public static String transCallTerm (Node call, String resultVar, CodeGenContext ctx); // TERM -> CALL
public static String transCallInstr(Node call, CodeGenContext ctx);                   // INSTR -> CALL (void functions)
```

Hooks, **one line each**:

- `TermGen.transTerm`: replace the `throw new UnsupportedOperationException("3b ...")` with `return CallGen.transCallTerm(first, resultVar, ctx);`
- `AlgoGen.transInstr`, `CALL` case: `return CallGen.transCallInstr(child, ctx);`

### 4.4 `ProgramGen` (skeleton by Lead, filled in by Person 3), new

```java
public static String transSplProg(Node splProg, CodeGenContext ctx); // P + "return 0;" + "}" + "/* end of executable C */"
static String transP(Node p, CodeGenContext ctx);
static String transFDecl(Node fDecl, CodeGenContext scratch);         // commented code-mine text
static String transFType(Node fType, CodeGenContext scratch);
static String asComment(String code);                                 // see watch-out 1
static void   declareUserVars(Node root, CodeGenContext ctx);         // walk every V_DECL, ctx.declareUserVar(ctx.lookUp(leaf))
static String detectCType(Node root);                                 // any NUM leaf containing '.' -> "float", else "int"
```

`GenKinds` (class `GenKinds`, in the file `GenKind.java`; owned by Lead) gains constants `P, V_DECL, F_DECL, F_TYPE, INPUT`, plus a helper `namesInVDecl(Node)` mirroring `NodeKinds.namesInVDeclChain` in the semantic package, which is package-private.

### 4.5 Output layout

```
/* begin of the non-executable code-mine for later inlining */
/* ... commented function code ... */
/* end of the non-executable code-mine */
/* begin of executable C code */
#include <iostream>
int main()
{
<user variable declarations>
<temp declarations>
<body of main ALGO>
return 0;
} /* end of executable C */
```

The body must be generated **before** `declarations()` is called, because the temp list is only complete afterwards. `CodeGenMain` already notes this.

## 5. Split of work

Roles: **Lead** (the author of `CodeGenContext`, `GenKinds` and `TermGen` in 3a), **Person 2** and **Person 3**.

The Lead writes the whole **skeleton** first (every new file with its signatures and `// TODO` bodies that throw `UnsupportedOperationException`, like the 3a stubs) and merges it. Person 2 and Person 3 only start once it compiles.

### 5.1 Who adds or changes which file

All paths are under `src/main/java/spl/` (main) or `src/test/java/spl/` (test).

| Person                                                | NEW files to add                                   | EXISTING files to change                      | What exactly                                                                                                                                                      |
| ----------------------------------------------------- | -------------------------------------------------- | --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Lead** (skeleton, step 1)                     | `codegen/FunctionInfo.java`                      |                                               | Data class only (fields from 4.2 plus a constructor). Complete, no TODOs.                                                                                         |
|                                                       | `codegen/FunctionRegistry.java`                  |                                               | Stubs: `build(Node root)`, `forCall(Node, SymbolTable)`.                                                                                                      |
|                                                       | `codegen/CallGen.java`                           |                                               | Stubs: `transCallTerm`, `transCallInstr`.                                                                                                                     |
|                                                       | `codegen/ProgramGen.java`                        |                                               | Stubs: `transSplProg`, `transP`, `transFDecl`, `transFType`, `asComment`, `declareUserVars`, `detectCType`.                                         |
|                                                       |                                                    | `codegen/CodeGenContext.java`               | New 3-arg constructor (keep the 2-arg one), `newVar(prefix)`, `declareUserVar`, `registry()`, `scratch()`, `declarations()` emits user vars then temps. |
|                                                       |                                                    | `codegen/GenKind.java` (class `GenKinds`) | Constants `P, V_DECL, F_DECL, F_TYPE, INPUT`; helper `namesInVDecl(Node)`.                                                                                    |
|                                                       |                                                    | `codegen/TermGen.java`                      | Replace the `throw ... "3b"` for `CALL` with `return CallGen.transCallTerm(first, resultVar, ctx);`.                                                        |
|                                                       |                                                    | `codegen/CodeGenMain.java`                  | Change the signature to take the `SPL_PROG` root (`generate(Node splProg, SymbolTable symT, String outPath)`) with a `// TODO` body. No logic.              |
|                                                       |                                                    | `test/.../CodeGenContextTest.java`          | Add tests for the new context methods.                                                                                                                            |
| **Person 2** (inlining engine) - Donovan       | `test/.../codegen/FunctionRegistryTest.java`     | `codegen/FunctionRegistry.java`             | Implement `build` (crawl every `F_TYPE`, key = id of its `#name` leaf) and `forCall`.                                                                     |
|                                                       | `test/.../codegen/CallGenTest.java`              | `codegen/CallGen.java`                      | Implement the inline procedure (section 5.2 below), the void and num cases, and a depth guard.                                                                    |
|                                                       |                                                    | `codegen/AlgoGen.java`                      | **One line** in the `CALL` case of `transInstr`: `return CallGen.transCallInstr(child, ctx);`. Only once `AlgoGen` is pushed to `main`.           |
| **Person 3** (program layer) -  Arytonn        | `test/.../codegen/ProgramGenTest.java`           | `codegen/ProgramGen.java`                   | Implement `SPL_PROG`, `P`, `F_DECL`/`F_TYPE` (commented mine via `ctx.scratch()`), `asComment`, `declareUserVars`, `detectCType`.                 |
|                                                       |                                                    | `codegen/CodeGenMain.java`                  | Fill in the body: generate body first, then `declarations()`, write the layout from 4.5.                                                                        |
|                                                       |                                                    | `Main.java`                                 | After the type analyser: call `ProgramGen.detectCType`, create the registry and context, call `CodeGenMain.generate`, write the output file.                  |
| **Lead** (step 2, after the skeleton is merged) | `tests/resources/codegen/*.spl.txt` (7 programs) |                                               | Fixtures listed in 5.3.                                                                                                                                           |
|                                                       | `test/.../codegen/CodegenE2ETest.java`           |                                               | Runs the compiler, compiles the output with `g++`, runs it, compares stdout, checks for duplicate labels.                                                       |
|                                                       |                                                    | `README.md`                                 | New section: new files, interfaces, call order.                                                                                                                   |

### 5.2 The inline procedure (Person 2)

1. `info = registry.forCall(call, symT)`.
2. Walk the `INPUT` chain and collect the `TERM` children (N arguments). Check `N == info.paramNameNodes.size()`.
3. For each argument: `p = ctx.newVar(); code += transTerm(arg, p, ctx)`.
4. For each parameter `i`: `code += ctx.lookUp(param_i) + " = " + p_i + ";\n"`. Evaluate **all** arguments first, then assign.
5. `code += AlgoGen.transAlgo(childNamed(info.bodyP, ALGO), ctx)`. This is the tree-based inline; labels come out fresh.
6. If num: `r = ctx.newVar(info.uniqueName); code += transTerm(info.returnTerm, r, ctx);` then, if `resultVar != null`, `code += resultVar + " = " + r + ";\n"`.
7. If void: no return step.

### 5.3 Test programs (Lead, step 2)

1. A num function with a `while` loop (Collatz-style) called **twice**. Checks unique labels.
2. A void function that prints.
3. A zero-argument function.
4. Nested: `f` declares `g` in its own `F_DECL` and calls it; `main` calls `f` twice.
5. A call inside `print ( ... )`, inside a `BOOL` condition, and as an argument of another call (`add ( #sq ( 2 ) #sq ( 3 ) )`).
6. A function with its own local variables.
7. A float program (`cType = float`).

Validators in the harness: no duplicate `Ln:` label definitions in the output, and every `goto` target is defined.

## 6. Order of work

1. **All (about 30 min):** agree on section 0 and the contracts in section 4.
2. **Lead** writes and merges the skeleton (compile order: `FunctionInfo`, `FunctionRegistry`, `CallGen`, `ProgramGen`, then `GenKind`, `CodeGenContext`, `TermGen`, `CodeGenMain`). Nobody else edits `codegen/` until it is merged.
3. **Person 2** and **Person 3** work in parallel, each only in their own files from the table.
4. **Lead** writes fixtures and the E2E harness while they work, and makes sure the finished `AlgoGen` gets pushed.
5. **Integrate** once `AlgoGen` is on `main`: Person 3's wiring, then Person 2's `AlgoGen` hook, then run the E2E suite and fix whichever file the failure points to.

## 7. Watch-outs

1. **Nested C comments.** The mine is wrapped in `/* ... */`, but the generated code already contains `/* if */`, `/* end */`, etc. The first inner `*/` would close the outer comment and leave the rest of the mine as live code. `asComment` must neutralise inner markers (for example replace `/*` with `/ *` and `*/` with `* /`) before wrapping. A `print` string containing `*/` is covered by the same replacement.
2. **Declare every user variable exactly once.** Parameters and locals of functions are never declared in the executable part by the mine (it is a comment), so `declareUserVars` must cover the whole tree, not only main's `V_DECL`.
3. **The return variable needs a declaration too.** Create it with `ctx.newVar(prefix)`, never by string concatenation, or it will be undeclared in C.
4. **`resultVar` can be absent.** For `INSTR → CALL` there is no continuation variable. Do not emit `null = r;`.
5. **Arguments before parameters.** Evaluate all `pN` first, then assign all parameters (matters when an argument reads a variable that is also a parameter of the callee).
6. **Only the function body's `ALGO` is inlined.** The callee's `V_DECL` is hoisted, and its nested `F_DECL` functions are only reachable through calls that appear in the body (resolved by the registry, which indexes every `F_TYPE` in the tree).
7. **Labels need a statement after them in C.** `GenKinds.label()` already appends ` ;`; keep using it.
8. **`std::cout` and `#include <iostream>`** are what the spec's 3a/3b text uses, so the generated file compiles with `g++`, not plain `gcc`.

## 8. Worked trace (illustrative numbering)

SPL: `#y = #square ( #x ) ;` with `#square` = `num #square ( #n ) { : : return ( mul ( #n #n ) ) }`; symbol table has `#x -> v1`, `#y -> v2`, `#n -> v3`, `#square -> f1`.

1. `transAssign`: `t1 = ctx.newVar()`, then `transTerm(TERM→CALL, "t1")`.
2. `CallGen`: argument `#x` goes into `p1 = t2`, giving `t2 = v1;`.
3. Parameter copy: `v3 = t2;`.
4. Body: the callee `ALGO` is empty, so `/* end */`.
5. Return variable `r = f1t3` (via `newVar("f1")`); `transTerm(mul(#n #n), "f1t3")` allocates `t4`, `t5`.
6. Continuation link: `t1 = f1t3;`, then the assignment finishes with `v2 = t1;`.

```c
t2 = v1;
v3 = t2;
/* end */
t4 = v3;
t5 = v3;
f1t3 = t4 * t5;
t1 = f1t3;
v2 = t1;
```

A second call to `#square` produces the same shape with new temps and, if the body had a loop, new labels.

## 9. Definition of done for 3b

- [ ] Section 0 items resolved (AlgoGen landed, `cType` detected, `Main` wired)
- [ ] Skeleton merged (Lead): all new files compile, `CodeGenContext` additions in
- [ ] `FunctionRegistry`, `CallGen` done and hooked into `TermGen` and `AlgoGen` (Person 2)
- [ ] `ProgramGen` and the new `CodeGenMain` emit the layout in 4.5, `Main.java` wired (Person 3)
- [ ] All seven E2E programs compile with `g++` and print the expected output
- [ ] No duplicate labels in any generated file
- [ ] README section listing new files, interfaces and call order
