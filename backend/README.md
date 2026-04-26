# study-the-spire-api (Kairo backend)

Kotlin **Kairo** HTTP API. Wire contract: [`../contracts/api/openapi.yaml`](../contracts/api/openapi.yaml).

## Requirements

- **JDK 21**
- **Gradle**: use the included `./gradlew` in this directory.
- **Kairo / Highbeam dependencies** resolve from [Google Artifact Registry](https://cloud.google.com/artifact-registry/docs). Authenticate for Gradle (e.g. [Application default credentials](https://cloud.google.com/docs/authentication/application-default-credentials) after `gcloud auth application-default login`, or a service account) so the `artifactregistry://` repository in `build.gradle.kts` can be read.

## Run (local)

```bash
cd backend
export CONFIG=development
./gradlew run
```

Smoke test:

```bash
curl -sS http://127.0.0.1:8080/hello
```

Expected:

```json
{"message":"the spire awaits"}
```

## Docker smoke test

```bash
cd backend
./gradlew clean installDist
docker build -t study-the-spire-api .
docker run --rm -p 8080:8080 -e PORT=8080 study-the-spire-api
```

In another terminal:

```bash
curl -sS http://127.0.0.1:8080/hello
```

Expected:

```json
{"message":"the spire awaits"}
```

Kairo also exposes health checks when `HealthCheckFeature` is enabled (see [Health Check](https://kairo.highbeam.com/modules/kairo-health-check/)):

- `GET /health/liveness`
- `GET /health/readiness`

## Configuration

- HOCON files live under `src/main/resources/config/`.
- The active file is `config/<CONFIG>.conf` where **`CONFIG` is a required environment variable** (e.g. `development`, `production`). See [Kairo Config](https://kairo.highbeam.com/modules/kairo-config/).

## References

- [Kairo: Getting started](https://kairo.highbeam.com/getting-started/)
- [Kairo Application (`kairo` / `startAndWait`)](https://kairo.highbeam.com/modules/kairo-application/)
