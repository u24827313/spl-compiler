# SPL Compiler — COS341 2026

Lexer + parser for the Students' Programming Language (SPL), producing a
`tree.xml` syntax tree per the practical spec.

## Prerequisites

You need a JDK (21+) and Maven installed. If you've never used Maven before:

**Check if you already have it:**

```bash
mvn -version
java -version
```

**If Maven isn't installed:**

- **WSL/Linux:** `sudo apt update && sudo apt install maven`
- **macOS:** `brew install maven`
- **Windows (not WSL):** [Download from maven.apache.org](https://maven.apache.org/download.cgi) and add `bin/` to PATH

**IDE setup (recommended over raw CLI if you're new to Maven):**

- **VS Code:** install the "Extension Pack for Java" — it auto-detects `pom.xml` and handles builds/tests through the UI, no CLI needed
- **IntelliJ:** File → Open → select the `spl-compiler` folder → it detects `pom.xml` automatically and prompts to import as a Maven project

## Build & run

```bash
mvn compile              # compile only
mvn test                 # run unit tests
mvn package               # build spl-compiler.jar with dependencies
mvn exec:java -Dexec.mainClass=spl.Main -Dexec.args="src/SPL.txt"
```

## Phase 3a code generation

Phase 3a translates an `ALGO` subtree into a **C++ fragment**, not a standalone program. `AlgoGen.transAlgo(algo, context)` supports any `ALGO` subtree and emits the main instructions, including print, assignment, branches, and all four `while`/`until` loop forms. `TermGen` and `BoolGen` provide the expression and condition translations.

`AlgoFragmentWriter.render(algo, symbols, cType)` returns hoisted temporary declarations followed by the translated body, using LF for in-process composition. `AlgoFragmentWriter.write(algo, symbols, cType, outputPath)` writes UTF-8 with the specification's ASCII-13 line endings. The caller supplies the numeric C++ type explicitly as `"int"` or `"float"`; Phase 2b currently does not expose a whole-program type-selection API.

The fragment intentionally has no `#include`, user-variable declarations, function definitions, or `main()`. It uses C++ `std::cout`; complete program generation and both forms of function `CALL` remain Phase 3b work. `Main` currently writes `tree.xml`, then runs semantic and type analysis; it does not yet invoke code generation. The native fragment tests wrap the output in a test-only C++ harness and skip when `g++` is unavailable.

## Project layout

```
src/main/java/spl/
  lexer/
    Token.java, TokenType.java     - shared token model
    Lexer.java                     - orchestrator, chains all recognizers
    recognizers/                   - ONE FILE PER LEXICAL CATEGORY (own these independently)
      NumRecognizer.java
      UserDefinedNameRecognizer.java
      StringRecognizer.java
      KeywordRecognizer.java
  parser/
    Parser.java, ParseException.java
  tree/
    Node.java, XmlTreeWriter.java  - builds tree.xml per spec's exact schema
  Main.java

src/test/java/spl/                 - JUnit 5 tests, mirrors main/ package structure
grammar/SPL-grammar.md             - LL(1) analysis, grammar transform decision, notes
tests/resources/valid|invalid/     - sample .spl.txt programs for regression testing
```

## Workflow

- Each lexical-category recognizer is independent — implement and unit-test
  yours in isolation, then integrate via `Lexer.java`.
- Open a PR into `main` per feature/category; at least one other member reviews.
- Before implementing the parser, finish the LL(1) decision in
  `grammar/SPL-grammar.md` — don't start coding against a grammar the group
  hasn't agreed on yet.
- Keep `tests/resources/` growing as you find edge cases (nested calls,
  dangling-else-shaped branches, empty V_DECL/F_DECL/ALGO, etc.).

## Status

- [ ] Lexer (per-category recognizers)
- [ ] Parser
- [ ] tree.xml writer
- [ ] Error messages with hints
- [ ] End-to-end test on sample SPL.txt files
