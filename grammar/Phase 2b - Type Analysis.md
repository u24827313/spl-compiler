
# Phase 2b — Type Analysis

Phase 2b fills the symbol table with type information and rejects any syntax tree that
violates the SPL type rules. It runs **after** phase 2a (scopes and names): by then every
user-defined name has been replaced by a unique system-generated new name, so two syntax
elements with the same new name are the same entity.

```
lexer -> parser -> 2a (scopes & names) -> 2b (type analysis) -> code generation
```

## Package `typeanalysis`

| File                         | Owner    | Responsibility                                                                                                                          |
| ---------------------------- | -------- | --------------------------------------------------------------------------------------------------------------------------------------- |
| `Type.java`                | Person 1 | Enum:`UNKNOWN`, `NUMERIC`, `BOOLEAN`, `PROCEDURE`, `OK`                                                                       |
| `TypeException.java`       | Person 1 | Error thrown on the first type violation (carries the offending node)                                                                   |
| `TypeContext.java`         | Person 1 | Shared state (node types, name types), dispatcher`check(Node)`, error helpers, the **only** class that touches the `Node` API |
| `TypeAnalyser.java`        | Person 1 | Entry point: pre-check, then crawl                                                                                                      |
| `ProgramTypeChecker.java`  | Person 1 | `SPL_PROG`, `P`, `V_DECL`, `F_DECL`, `F_TYPE`, `ALGO` — the only checker that **writes** name types                  |
| `ExprTypeChecker.java`     | Person 2 | `TERM`, `BOOL`, `CALL`, `INPUT`                                                                                                 |
| `InstrTypeChecker.java`    | Person 3 | `INSTR`, `OUTP`, `ASSIGN`, `BRANCH`, `LOOP`, `COND`                                                                         |
| `MixedNumberPreCheck.java` | Person 3 | Tree-wide`mod` / `div` / decimal-dot rule (implemented in the skeleton)                                                             |

Each person edits **only their own files**. Anything in the shared files (`Type`,
`TypeContext`, `TypeAnalyser`) goes through Person 1.

## How it works

1. `TypeAnalyser.analyse(root)` first runs `MixedNumberPreCheck`. The spec says to reject
   such trees outright, without finer analysis, so this happens before the crawl.
2. It then calls `ctx.check(root)`. `TypeContext.check` looks at the node kind and hands the
   node to the checker that owns it. Checkers call `ctx.check(child)` on the children they
   need (post-order: children first, then the parent's own rule), so no checker imports
   another one.
3. `check` records the resulting type for the node (`ctx.typeOf(node)`) and returns it.
4. On the first violated rule a `TypeException` is thrown (fail-fast). If the whole tree is
   crawled without an exception, the tree is type-correct and ready for code generation.

### Contracts (do not change without telling the others)

```java
// every checker
public Type check(Node n);

// TypeContext
Type  check(Node n);                              // dispatch + record
Type  typeOf(Node n);                             // UNKNOWN if not yet checked
void  setType(Node n, Type t);
Type  nameType(String newName);                   // UNKNOWN if not yet typed
void  setNameType(String newName, Type t);        // writes the name's type
void  require(Node n, Type actual, Type expected, String rule);  // throws TypeException
TypeException error(Node n, String message);
String kind(Node n); List<Node> children(Node n); String text(Node n);
boolean isOperator(Node n, String op);            // e.g. isOperator(n, "mod")
```

### Design decisions

- **Node types live in `TypeContext`** (identity map), so `Node.java` is not modified.
- **Name types are keyed by the system-generated new name** from phase 2a. They are stored in
  `TypeContext` for now. TODO: once the 2a `SymbolTable` API is confirmed, forward
  `nameType` / `setNameType` to it so the symbol table itself carries the types.
- **Fail-fast error policy:** one `TypeException` per run, carrying the node.
- **Function names are typed from their header before the body is checked**
  (`void` -> `procedure`, `num` -> `numeric`), so recursive calls do not hit "unknown".
  *Proposed: confirm with the team and against the 2a scope rules.*
- **No arity check on calls:** the spec only requires every argument to be numeric.

## Rules by file

The full rule text is in the Javadoc at the top of each checker. Summary of what writes and
what only checks:

- `V_DECL -> NAME V_DECL`: `NAME` becomes `numeric`.
- `F_TYPE -> void NAME (...)`: `NAME` becomes `procedure`; `F_TYPE -> num NAME (...)`: `NAME`
  becomes `numeric` (and the returned `TERM` must be `numeric`).
- `CALL -> NAME ( INPUT )`: type of `CALL` = type of `NAME`, and `NAME` must not be `unknown`.
  `INSTR -> CALL` requires `procedure`; `TERM -> CALL` requires `numeric`.
- `TERM` is `numeric`, `BOOL` is `boolean`, everything else is `ok` once its children are fine.

## Error messages

| Situation                                             | Message                                                                               |
| ----------------------------------------------------- | ------------------------------------------------------------------------------------- |
| tree contains both a`mod` and a `div` node        | `A float-integer-conflict might perhaps be possible` (fixed by the spec)            |
| tree contains`mod` and a `NUM` with a decimal dot | `A NUM with a decimal dot is not allowed in a program that uses mod` (our wording)  |
| any other violated rule                               | `Type error in rule <RULE>: expected <type> but found <type>` (via `ctx.require`) |

## Wiring in `Main`

```java
SyntaxTree tree = parser.parse(tokens);
// ... phase 2a ...
try {
    new TypeAnalyser().analyse(tree.getRoot());   // adapt to the real SyntaxTree API
} catch (TypeException e) {
    System.err.println(e.getMessage());
    System.exit(1);
}
```

To make the 2a symbol table carry the types, pass one shared `TypeContext` (or the 2a
symbol table, once forwarding is in place) instead of the no-argument constructor.

## Adapting the skeleton to the repo

- `Node` (`spl.tree.Node`) only exposes `getContents()`, so `TypeContext.kind()` reads it as the
  grammar symbol for internal nodes and classifies leaves itself (`NUM`, `STRING`, `NAME`, a
  keyword, or punctuation). The leaf patterns are marked `VERIFY` in `TypeContext`; check them
  against what the lexer and phase 2a actually put in a leaf (number format, whether string
  quotes are kept, the format of the new names).
- Everything that touches `Node` is in `kind`, `children`, `text` and `isOperator` in
  `TypeContext`; fix those and nothing else if the tree format changes.
- Before coding the checkers, confirm whether keyword tokens (`mod`, `void`, `(` ...) appear as
  leaf children in the tree; it decides how each checker finds the operator and its operands.

## Testing

JUnit 5 under `src/test/java/typeanalysis/`. Each person tests their own checker on small
hand-built trees, then everyone runs full SPL programs through `Main`. Minimum cases:

- a valid program that passes without any exception
- `mod` and `div` in the same program
- `mod` together with a decimal `NUM`
- assigning to a procedure name
- `print` of a boolean expression
- a numeric function used as a statement (`INSTR -> CALL` needs `procedure`)
- a procedure used inside a `TERM`
- a call to a function that is not yet typed

## Workflow

1. Person 1 merges the skeleton (this package) first.
2. Persons 1 to 3 implement their own files on separate branches and write their own tests.
3. Merge, wire up `Main`, run the full-program tests together.
