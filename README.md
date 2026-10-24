# Platform SDK Kt

[![CI](https://github.com/qualithm/platform-sdk-kt/actions/workflows/ci.yaml/badge.svg)](https://github.com/qualithm/platform-sdk-kt/actions/workflows/ci.yaml)
[![codecov](https://codecov.io/gh/qualithm/platform-sdk-kt/graph/badge.svg)](https://codecov.io/gh/qualithm/platform-sdk-kt)
[![Maven Central](https://img.shields.io/maven-central/v/com.qualithm/platform)](https://central.sonatype.com/artifact/com.qualithm/platform)

Kotlin client for the Qualithm Platform API, generated from its OpenAPI spec. Every operation and
type comes from the spec the API serves, so the client changes when the API does.

## Features

- **Every management operation** — one typed method per operation, grouped by resource
  (`DevicesApi`, `AutomationsApi`, …), with the spec's names
- **One error type** — every failure is a `PlatformError` carrying the API's stable `code`
- **Configured from the environment** — `QUALITHM_API_TOKEN` and `QUALITHM_API_URL`
- **JVM and Android** — OkHttp for transport, Moshi for JSON

## Installation

Add the library from Maven Central:

```kotlin
dependencies {
    implementation("com.qualithm:platform:0.1.0")
}
```

## Quick Start

Create an API token in the Qualithm app, on the team page under **API tokens**, then:

```kotlin
import com.qualithm.platform.PlatformClient
import com.qualithm.platform.generated.apis.DevicesApi
import com.qualithm.platform.generated.apis.TeamsApi

val platform = PlatformClient(token = "<your-api-token>")

// An API token belongs to one team, so its team list has exactly that team.
val teamId = platform.call { api(::TeamsApi).listTeams() }.data.items.first().id

val page = platform.call { api(::DevicesApi).listDevices(teamId) }

for (device in page.data.items) {
    println(device.name)
}
```

## Usage

### Configure the client

`PlatformClient` reads `QUALITHM_API_TOKEN` and `QUALITHM_API_URL` when you don't pass `token` or
`baseUrl`. The base URL defaults to the production API, `https://api.qualithm.com`.

```kotlin
val platform = PlatformClient(baseUrl = "https://api.qualithm.com")
```

Pass `httpClient` to send requests through your own `OkHttpClient`. The client adds the token to a
copy of it.

### Call an operation

`api(::SomeApi)` returns that resource group, bound to your token. Run calls inside
`platform.call { … }`, which turns every failure into a `PlatformError`. Method names match the
spec's `operationId`s. Every response is the API's envelope, so the result is in `data`. Operations
on a team's resources take its `teamId`:

```kotlin
import com.qualithm.platform.generated.apis.AutomationsApi

val automations =
    platform.call { api(::AutomationsApi).listAutomations(teamId, page = 1, limit = 50) }
```

Calls block the calling thread. On Android, make them off the main thread.

### Error Handling

`platform.call` throws only `PlatformError`. Branch on `code`, never on `message`. A code this
version doesn't know yet reads as `UNKNOWN_DEFAULT_OPEN_API`, and a request that got no response is
a `PlatformError.Transport` with `status` `0`:

```kotlin
import com.qualithm.platform.PlatformError
import com.qualithm.platform.generated.models.ApiErrorCode

try {
    platform.call { api(::DevicesApi).getDevice(teamId, deviceId) }
} catch (error: PlatformError) {
    if (error.code == ApiErrorCode.NOT_FOUND) {
        // the device doesn't exist, or isn't in this team
    }
    throw error
}
```

## API Reference

The operations and types are generated from the platform's OpenAPI spec, committed as
[`openapi.json`](openapi.json). Build the HTML reference with Dokka, into `build/dokka/html/`:

```bash
./gradlew dokkaGenerate
```

## Examples

See [`examples/`](examples/README.md):

| Example                                       | Description                                                        |
| --------------------------------------------- | ------------------------------------------------------------------ |
| [`listdevices`](examples/listdevices/Main.kt) | List the devices in the token's team, and report a `PlatformError` |

```bash
QUALITHM_API_TOKEN=<your-api-token> ./gradlew runExample -Pexample=listdevices
```

## Development

### Prerequisites

- JDK 21, on `JAVA_HOME`
- [Docker](https://www.docker.com/), to regenerate the client

Gradle comes from the wrapper (`./gradlew`).

### Regenerating

`src/main/kotlin/com/qualithm/platform/generated/` comes from `openapi.json` and the generator
pinned in `scripts/generate`. Never edit it by hand: CI regenerates it and fails on any difference.
The platform opens a pull request here with the new spec and the regenerated code whenever its API
changes. To regenerate by hand:

```bash
scripts/generate
```

The hand-written layer is `PlatformClient.kt` and `PlatformError.kt` in `com.qualithm.platform`.

The generated directory compiles as its own Gradle source set, `generated`, whose classes and
sources ship in the one jar. That source set compiles without `allWarningsAsErrors`, and ktfmt,
detekt and the Kover gate exclude it in `build.gradle.kts`. Dokka documents it without requiring
KDoc on every declaration.

### Building

```bash
./gradlew assemble
```

### Testing

```bash
./gradlew test koverXmlReport
```

### Linting & Formatting

```bash
./gradlew ktfmtCheck detekt
./gradlew ktfmtFormat
```

### Checks

`.claude/rules/checks.md` lists the commands CI runs; `.githooks/pre-commit` runs them.

### Dependencies

Versions live in `gradle/libs.versions.toml`. After changing one, rewrite the lockfile and commit
it:

```bash
./gradlew dependencies --configuration runtimeClasspath --write-locks
```

## Supported Platforms

Any JVM 17 or later, and Android. Built and tested with JDK 21, Gradle 9.8 and Kotlin 2.4.

## License

Apache-2.0
