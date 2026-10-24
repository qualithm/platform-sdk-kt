# Examples

Each example reads the API token from `QUALITHM_API_TOKEN`. Create one in the Qualithm app, on the
team page under **API tokens**.

| Example                              | Description                                                        |
| ------------------------------------ | ------------------------------------------------------------------ |
| [`listdevices`](listdevices/Main.kt) | List the devices in the token's team, and report a `PlatformError` |

```bash
QUALITHM_API_TOKEN=<your-api-token> ./gradlew runExample -Pexample=listdevices
```
