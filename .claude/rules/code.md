# Kotlin Code Guidelines

ktfmt (KotlinLang style) and detekt enforce layout, naming and most idioms. Fix what they flag; this
file covers what they can't. CI is generated from `dx/ci-templates/kotlin-lib.yaml`.

## Layout

- One plain Kotlin/JVM library, one Gradle project at the repo root: sources in `src/main/kotlin/`,
  tests in `src/test/kotlin/**/*Test.kt`, runnable examples in `examples/<name>/Main.kt`.
- The library uses `explicitApi()`: every public declaration states its visibility, and carries
  KDoc, since Dokka fails on an undocumented one.
- One version catalog, `gradle/libs.versions.toml`, holds every dependency and plugin version. Build
  scripts reference `libs.*` and never write a version inline.
- `gradle.lockfile` locks the runtime classpath that ships. Rewrite it with the dependency change.

## Kotlin

- Errors are a `sealed` hierarchy thrown across the public API; `Result` stays internal. Messages
  are lowercase with no trailing punctuation.
- Prefer immutable `data class`es and `val`. Model a closed set of states with a `sealed interface`
  and an exhaustive `when`, never with nullable flags.
- Coroutines: structured concurrency only. Take a `CoroutineScope` or `CoroutineDispatcher` from the
  caller; never `GlobalScope`, never `runBlocking` outside tests and examples.
- Keep the public surface small: `internal` by default, `public` only for what a consumer calls.

## Generated and synced files

- `.github/workflows/` is generated from `dx/ci-templates/`; change the template and run the dx
  sync, never edit it here. `CI Required` is the single required status check.

## Releases

`version` in `gradle.properties` is the version source, bumped with `dx tool version-bump`. A
release publishes to Maven Central with its sources and javadoc jars, signed.

## When code changes

A behavior change carries tests covering its new lines, and a public API change updates its KDoc.
