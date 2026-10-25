---
description: "Runtime conventions for errors, logs, settings and CLIs"
paths:
  - "**/*.kt"
---

# Runtime Conventions

One set of rules for errors, logs, settings and CLIs holds in every language, so logs and errors
from different repos read and search together. This is how they look here.

## Errors

- Errors are a `sealed` hierarchy under one `<Component>Error` base, thrown across the public API.
  Messages are lowercase, with no trailing punctuation, and name what failed (`claim code expired`).
- A `Result` or sealed result type is only for an expected outcome inside the library, never in a
  public signature.

## Logging

- Library code never writes to stdout or stderr and adds no log handlers. Where it logs, it takes
  the caller's logger.
- A log line is a `snake_case` event plus fields, never a sentence with values interpolated.
- Field keys are `snake_case` in every language, so one query reads every service. The shared names
  are `team_id`, `device_id`, `request_id`, `trace_id`, `duration_ms`, and `error` for the error,
  never `e` or `err`.

## Settings

- Settings types are `<Component>Config` with production defaults, and units go in names
  (`timeoutMs`, `maxBodyBytes`).
- Env vars it reads for its own settings start with `QUALITHM_`, because it runs on a builder's
  machine or device. Standard names (`PORT`, `LOG_LEVEL`) stay unprefixed, and credentials keep
  their own naming rule.
