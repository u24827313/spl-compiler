# How We're Splitting the Parser Code

## The problem in plain terms

We split the grammar into three chunks by rule (that part's easy). The hard
part is: **the rules call each other constantly.** `TERM` needs `CALL`.
`BRANCH` needs `ALGO`. `ALGO` needs `INSTR`. `INSTR` needs `ASSIGN`,
`BRANCH`, `LOOP`, and `CALL`. There's no clean wall between our three
pieces — they're tangled together, because the grammar itself is tangled
together.

So we can't just write three totally separate, independent files that never
talk to each other. But we also don't want one giant file that all three of
us are editing at the same time (that's a recipe for constant merge
conflicts).

**The fix:** three files, one per person/cluster, but they're allowed to
call each other's methods. We just need to agree in advance on the exact
method names each file exposes — that agreement *is* the thing that lets us
work in parallel without stepping on each other.

## The three files

| File                   | Owns these grammar rules                                                    |
| ---------------------- | --------------------------------------------------------------------------- |
| `ProgramParser.java` | `SPL_PROG`, `P`, `V_DECL`, `F_DECL`, `F_TYPE`, `ALGO`, `OUTP` |
| `InstrParser.java`   | `INSTR`, `CALL`, `INPUT`, `ASSIGN`                                  |
| `ExprParser.java`    | `TERM`, `BRANCH`, `BOOL`, `LOOP`, `COND`                          |

Think of each file as **one person's toolbox**. Inside your toolbox, you
write one method per grammar rule you own. Example: whoever owns
`InstrParser.java` writes a method called something like
`parseInstr(...)` that handles the `INSTR` rule, another called
`parseCall(...)` for `CALL`, and so on.

## How the "calling each other" part works

Every method takes the same one extra thing as a parameter: a shared
object called `ParserContext`. Think of `ParserContext` as **the shared
whiteboard** — it holds:

- where we currently are in the list of tokens
- a way to grab the next token / check what it is
- a way to create new tree nodes with unique IDs

Every parsing method — no matter which of the three files it lives in —
takes this whiteboard as an argument, reads from it, and writes to it.
That's what lets `ExprParser`'s `TERM` method call `InstrParser`'s `CALL`
method directly: they're both just functions that take the same
whiteboard.

```
InstrParser.parseCall(ctx)     <- "ctx" is the shared whiteboard
ExprParser.parseTerm(ctx)
ProgramParser.parseAlgo(ctx)
```

You don't need to fully understand Java's `static` keyword to get the
idea — it just means "this method doesn't belong to one specific object,
you can call it directly by file name, like `InstrParser.parseCall(...)`."

## The one thing we MUST agree on before splitting up

**The exact method names and what they return**, for every rule that
another file needs to call into. This list is the actual contract between
us. For example:

- `ExprParser.parseTerm(ctx)` — needed by `InstrParser` (since `ASSIGN`
  uses `TERM`)
- `InstrParser.parseCall(ctx)` — needed by `ExprParser` (since `TERM` can
  be a `CALL`)
- `ExprParser.parseBool(ctx)` — needed by `ProgramParser`? (check — does
  `ALGO` ever touch `BOOL` directly, or only through `BRANCH`/`LOOP`?)
- `ProgramParser.parseAlgo(ctx)` — needed by `ExprParser` (since `BRANCH`
  and `LOOP` both contain `{ ALGO }`)

**Action item for the group chat:** go through the grammar together and
write down, for every arrow that crosses from one person's rules into
another person's rules, exactly one line: `method name -> who calls it`.
Once that list exists, we can all go write our own file without waiting
on each other, because we already know what shape the other two files
will expose.
