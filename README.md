# SPL Compiler — COS341 2026

Lexer + parser for the Students' Programming Language (SPL), producing a
`tree.xml` syntax tree per the practical spec.

## Build & run

```bash
mvn compile              # compile only
mvn test                 # run unit tests
mvn package               # build spl-compiler.jar with dependencies
java -jar target/spl-compiler.jar path/to/SPL.txt
```

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
- [ ] Grammar decision (left-factor vs SLR) documented
- [ ] Parser
- [ ] tree.xml writer
- [ ] Error messages with hints
- [ ] End-to-end test on sample SPL.txt files
