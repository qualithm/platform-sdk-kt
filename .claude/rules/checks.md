---
description: "Exact pre-commit commands for the kotlin-lib CI archetype, kept in sync with ci.yaml"
---

# Pre-commit Checks

This repo's `ci.yaml` is generated from `dx/ci-templates/kotlin-lib.yaml` via `dx ci sync` (check
for drift with `dx ci drift`). Run these before committing so CI passes on the first try:

```bash
./gradlew ktfmtCheck   # `./gradlew ktfmtFormat` rewrites the files
./gradlew detekt
python3 .claude/checks/ci-jobs.py --skip-ci-only test build docs
```

Every command runs from the Gradle CLI and needs a JDK 21 (`JAVA_HOME`). Gradle comes from the
wrapper.

The `ci-jobs.py --skip-ci-only test build docs` step runs CI's Test & Coverage, Build and Docs jobs
locally, minus the Codecov upload:

- **Test & Coverage** runs the tests under Kover and gates line coverage at 80%. The gate measures
  the library's own sources: the examples, and any generated code, are excluded in the Kover config
  of `build.gradle.kts`, never by lowering the bound.
- **Build** runs `./gradlew assemble`: the jar, the sources jar and the examples.
- **Docs** runs Dokka, which fails on a warning, so every public declaration needs KDoc. CI runs it
  on PRs targeting `main` only.

A dependency change rewrites the lockfile of the runtime classpath that ships; commit it with it:

```bash
./gradlew dependencies --configuration runtimeClasspath --write-locks
```
