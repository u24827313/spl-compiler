
## Phase 2a: Semantic Analysis (scopes and names)

After the parser produces the syntax tree, the semantic analyser crawls it, checks the scope and name rules from the spec, throws an error on any violation, and (for valid programs) fills a symbol table with a unique system-generated name (`v1`, `v2`, ... for variables and parameters, `f1`, `f2`, ... for functions) for every distinct entity.

### New files

```
src/main/java/spl/
  semantic/
    SemanticAnalyser.java     Crawler: walks the tree, drives the checkers          [Person 3]
    NodeKinds.java            Name constants + tree helpers (chain walking, name test) [Person 3]
    SemanticException.java    Thrown on any rule violation (message + node id)      [Person 2]
    NameGenerator.java        Hands out v1.. (variables) / f1.. (functions)                             [Person 2]
    Scope.java                One scope level: variable table, function table, level [Person 1]
    SymbolEntry.java          One entity: name, kind, level, declaring node, new name [Person 1]
    SymbolTable.java          Scope stack + nodeId -> SymbolEntry map               [Person 1]
    VariableChecker.java      Variable rules (declare, masking, nearest lookup)     [Person 1]
    FunctionChecker.java      Function rules (declare, unique, same-level calls)    [Person 2]
  Main.java                   Modified: run SemanticAnalyser after the parser       [Person 3]

src/test/java/spl/semantic/                    JUnit 5, one test class per file above (owner of the file writes it)
tests/resources/semantic/valid|invalid/        Sample .spl.txt programs, one per rule  [Person 2]
```

### Ownership

| Person                        | Owns                                                                         | Spec section                                   |
| ----------------------------- | ---------------------------------------------------------------------------- | ---------------------------------------------- |
| **Person 1: variables** | `VariableChecker`, `Scope`, `SymbolEntry`, `SymbolTable`             | Variable Names                                 |
| **Person 2: functions** | `FunctionChecker`, `NameGenerator`, `SemanticException`, test programs | Function Names                                 |
| **Person 3: crawler**   | `SemanticAnalyser`, `NodeKinds`, `Main.java` wiring                    | Unique Re-naming, Symbol Table, Error Messages |

### Interfaces (agreed contract: do not change without telling the team)

The real `Node` API (`spl.tree.Node`): `getId()`, `getContents()`, `getChildren()`, `getParent()`, `isLeaf()`. There is no separate type field: a node's kind is its `getContents()` string (the nonterminal name on internal nodes, the token text on leaves). The checkers read a name with `nameNode.getContents()`, so it is not passed separately.

```java
// ---- SymbolEntry (Person 1) ----
enum Kind { VAR, PARAM, FUNC }
class SymbolEntry {
    String originalName; Kind kind; int level; int declNodeId; String uniqueName;
}

// ---- Scope (Person 1) ----
class Scope {
    final int level;
    void putVar(String name, SymbolEntry e);   SymbolEntry getVar(String name);
    void putFunc(String name, SymbolEntry e);  SymbolEntry getFunc(String name);
    boolean hasName(String name);              // a variable OR a function with this name is declared in THIS scope
}

// ---- SymbolTable (Person 1) ----
class SymbolTable {
    SymbolTable(NameGenerator names);
    NameGenerator names();                 // the checkers use this to create new names
    void pushScope(int level);
    void popScope();
    Scope currentScope();
    SymbolEntry lookupVar(String name);    // walks the stack, innermost first
    SymbolEntry lookupFunc(String name);   // TOP scope only (no walking outward)
    void record(int nodeId, SymbolEntry e);   // node -> entity, for the code generator
    SymbolEntry getEntry(int nodeId);
}

// ---- VariableChecker (Person 1) ----
class VariableChecker {
    void declareParameter(Node nameNode, SymbolTable t);
    void declareVariable(Node nameNode, SymbolTable t);   // duplicate / masks parameter -> SemanticException
    void resolveVariableUse(Node nameNode, SymbolTable t); // no declaration -> SemanticException
}

// ---- FunctionChecker (Person 2) ----
class FunctionChecker {
    void declareFunction(Node nameNode, SymbolTable t);    // duplicate in this scope -> SemanticException
    void resolveFunctionCall(Node nameNode, SymbolTable t); // not in top scope -> SemanticException
}

// ---- NameGenerator / SemanticException (Person 2) ----
class NameGenerator { String next(SymbolEntry.Kind kind); }  // VAR/PARAM -> "v1", "v2", ...; FUNC -> "f1", "f2", ... (one counter per prefix; prefixes are constants)
class SemanticException extends RuntimeException {
    SemanticException(String message, int nodeId);
}

// ---- SemanticAnalyser (Person 3) ----
class SemanticAnalyser {
    SymbolTable analyse(SyntaxTree tree) throws SemanticException;
}
```

Rules for the checkers:

- `declare*` creates a `SymbolEntry`, takes a fresh name from the `NameGenerator`, stores it in the current scope, and calls `record(nodeId, entry)`.
- `resolve*` finds the matching entry and calls `record(useNodeId, thatSameEntry)`, so uses share the declaration's new name.
- A variable and a function may not share a name at the same level. Both `declareVariable` and `declareFunction` check `Scope.hasName` (variables are declared first, so in practice the clash is caught in `declareFunction`). Use a different message for each case: duplicate variable, local masks parameter, variable/function name clash.
- A local that collides with a parameter is caught in `declareVariable` because the parameter is already in the same scope (check the existing entry's kind to give a "masks parameter" message instead of a plain "duplicate").

### Reading the tree (based on a real `tree.xml`)

Facts about the tree shape that the crawler must handle. The test trees will differ, so handle these **generally** and never hard-code one program.

1. **Declaration lists are right-recursive chains, not flat lists.** `V_DECL -> #x V_DECL` ends in an empty `V_DECL`; `F_DECL -> F_TYPE F_DECL` ends in an empty `F_DECL`; `ALGO -> INSTR ; ALGO` ends in an empty `ALGO`. To collect every name at a level, walk the chain until you reach the empty node.
2. **Empty nodes are leaves.** An empty `V_DECL`/`F_DECL`/`ALGO` has contents but no children, so `isLeaf()` does **not** mean "this is a token". A user-defined name is a leaf whose contents start with `#` (`NodeKinds.isUserDefinedName`), and the `#` is part of the name (`#x`).
3. **A function is one `F_TYPE` node.** Its children, found by **contents rather than index**: the type (`num`/`void`), the `#name`, `(`, one `V_DECL` (the parameters), `)`, `{`, one `P` (the body), `return`, optionally a `TERM` (the returned value; only `num` functions have one), then `}`. `void` and `num` functions have different child counts (9 and 12 in the sample), so indexes are unreliable.
4. **The return `TERM` belongs to the function body's scope**, but it is a child of `F_TYPE`, not of the body's `ALGO`. It uses parameters (e.g. `return ( mul ( #n #n ) )`), so it must be resolved **before** `popScope()` for that body.
5. **Only a `P` opens a scope.** The root `P` is level 0 and each function-body `P` is its enclosing level + 1. `BRANCH` and `LOOP` bodies contain an `ALGO` but no `P`, so they stay in the current scope.
6. **Classifying a `#name` leaf inside an `ALGO` or a return `TERM`:** it is a function call if it is the first child of a `CALL` node, and a variable use otherwise (assignment targets sit directly under `ASSIGN`, other uses under `TERM`). A `CALL` can appear as an `INSTR` or nested inside a `TERM`, and its arguments (`INPUT -> TERM INPUT`) can contain more variables or calls.
7. **Nothing is renamed in place.** `Node.contents` is final, so the new names live in the `SymbolTable`, keyed by node id (this relies on `IdGenerator` giving every node a unique id).

Helpers for `NodeKinds` (crawler owner):

```java
static final String P = "P", V_DECL = "V_DECL", F_DECL = "F_DECL", F_TYPE = "F_TYPE",
                    ALGO = "ALGO", TERM = "TERM", CALL = "CALL";
static boolean isUserDefinedName(Node n);          // n.isLeaf() && contents starts with "#"
static Node childNamed(Node n, String contents);   // first child with those contents, or null
static List<Node> namesInVDeclChain(Node vdecl);   // all #name leaves along the chain
static List<Node> fTypesInFDeclChain(Node fdecl);  // all F_TYPE nodes along the chain
```

### Call order (the crawler must follow this exactly)

`analyseP(Node p, int level, Node enclosingFType)`, with `enclosingFType == null` for the root `P`. For each `P` at level `l`:

1. `pushScope(l)`. If `enclosingFType != null`, call `declareParameter` for each name in its parameter `V_DECL` chain **first**.
2. `declareVariable` for every name in this `P`'s own `V_DECL` chain.
3. `declareFunction` for the name of every `F_TYPE` along this `P`'s `F_DECL` chain (before looking at any `ALGO`).
4. Crawl this `P`'s `ALGO` subtree, including any nested `BRANCH`/`LOOP` `ALGO`s (same scope): the first child of a `CALL` goes to `resolveFunctionCall`, every other `#name` leaf goes to `resolveVariableUse`. If `enclosingFType != null`, also crawl its return `TERM` here, while this scope is still on the stack.
5. For each `F_TYPE` from step 3, call `analyseP(its body P, l + 1, thatFType)`.
6. `popScope()`.

Why the order matters:

- **Parameters before locals (step 1 before 2):** the masking check only works if the parameter is already in the scope.
- **A function's name belongs to the scope that contains its `F_DECL` (level `l`); its parameters belong to its body's scope (level `l + 1`).** Do not mix these up.
- **Functions are looked up in the top scope only.** A function body (level `l + 1`) therefore cannot call a function at level `l`, which is what rules out direct and indirect recursion.
- **Variables are looked up down the whole stack**, so a function body can still use variables declared at outer levels.
- **The return `TERM` is resolved inside the body's scope (step 4)**, otherwise its parameter uses would look undeclared.
- **Every function body's scope is popped (step 6) before the next sibling function is analysed (step 5).** This is what stops a declaration in one function from being visible "sideways" in another (Announcement A#21). Only ancestors are ever on the stack together, so a variable declared in `g` is visible in `g`'s sub-functions but not in `h` or in `h`'s sub-functions.

Worked trace on the sample tree (the exact name numbers depend on call order and don't matter, only uniqueness does):

| Step                     | Action                                                                                                  | Result                                           |
| ------------------------ | ------------------------------------------------------------------------------------------------------- | ------------------------------------------------ |
| level 0, steps 1 to 3    | declare`#x` (node 3), `#y` (5); functions `#square` (11), `#greet` (39)                         | `v1`, `v2`, `f1`, `f2`                   |
| level 0, step 4          | main`ALGO`: `#x` (79, 95), `#y` (87, 106, ...); `CALL` names `#square` (91), `#greet` (102) | uses take`v1`/`v2`; calls take `f1`/`f2` |
| `square` body, level 1 | parameter`#n` (14); empty `V_DECL`/`F_DECL`/`ALGO`; return `TERM` uses `#n` (30, 32)        | `#n` = `v3`, both uses `v3`                |
| `greet` body, level 1  | parameter`#who` (42); `print ( #who )` (62)                                                         | `#who` = `v4`, use `v4`                    |

### Wiring in `Main.java`

```
tokens  -> Parser -> SyntaxTree -> SemanticAnalyser.analyse(tree) -> SymbolTable
                                        |
                                        +-- throws SemanticException on any violation (print message, exit non-zero)
```

### Test programs (`tests/resources/semantic/`)

Invalid (each must throw): undeclared variable, undeclared function, duplicate variable at one level, duplicate function at one level, local masking a parameter, call to a function from the wrong level, direct recursion, indirect recursion, a variable and a function sharing a name at one level, a variable declared in one function and used "sideways" in a sibling function (A#21, with no other declaration available).

Valid (each must throw nothing): shadowed variable (nearest declaration wins), helper function copied into two functions (`h` inside both `f` and `g`), nested functions at several levels, a `num` function whose return term uses its parameter, a `void` function, empty declaration lists, variables and calls inside nested `if`/`while` bodies, two sibling functions that each declare `x` (different new names), and the A#21 case where the `x` in `j` resolves to `g`'s declaration while the `x` in `k` resolves to a different declaration in an ancestor of `k`.

### Decisions and open questions

Decided:

- A variable and a function may not share a name at the same level. This is stricter than the spec's literal wording (which only compares variables with variables and functions with functions), so it is a single check in `Scope.hasName` that is easy to relax.
- New names are `v1, v2, ...` for variables and parameters and `f1, f2, ...` for functions, with one counter per prefix. The spec only requires that the same entity gets the same name, different entities get different names, and the names are stored in the symbol table; the format (`sys132` in the spec) is only an example.
- Announcement A#21: a declaration is never visible "sideways" in a function hierarchy, only in the declaring function and its descendants. The scope stack handles this, as described under "Why the order matters".

Still to confirm with the lecturer:

- The return `TERM` is treated as part of the function body's scope (the sample program, `return ( mul ( #n #n ) )`, only works this way). The spec only talks about `ALGO`.
- Whether a variable and a function with the same name at one level must be rejected or allowed.
- Function-name uniqueness is read as "unique along one `F_DECL` chain" (the functions declared by one `P`), because the spec's own helper example needs two functions named `h` in different parents.
- Whether the symbol table must be printed or written to a file for later phases.
