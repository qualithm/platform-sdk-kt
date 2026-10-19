---
description: "Conventions for runnable examples"
paths:
  - "examples/**"
---

# Example Conventions

## File Structure

Each example is `examples/<name>/Main.kt`, where `<name>` is a lowercase package name
(`basicusage`), compiled in the `examples` source set against the library:

1. A header comment: one line saying what the example shows, then how to run it
   (`//     ./gradlew runExample -Pexample=basicusage`)
2. `package <name>`, matching the directory
3. Imports, the library's last
4. Helper types and functions, each with a KDoc comment
5. `fun main()` as the single entry point

## Conventions

- Use the public API only.
- Print what happens, so running the example shows the behaviour.
- Handle the errors the example is about with `onFailure` and a `when` over the error's types; let
  any other error end the run.
- The examples are outside the coverage gate and the API docs, but `./gradlew assemble` compiles
  them, and ktfmt and detekt check them.

## Running Examples

```bash
./gradlew runExample -Pexample=EXAMPLE_NAME
```

## README

`examples/README.md` has:

1. `# Examples` heading
2. A one-line intro
3. An `| Example | Description |` table linking each `Main.kt`
4. The `./gradlew runExample` command
