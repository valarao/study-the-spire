# Study the Spire — contracts

Shared wire formats between the **C# mod**, **Kotlin API**, and **Next.js** app. This directory is the source of truth for HTTP surface area and JSON payloads.

## Layout

| Path | Purpose |
|------|---------|
| [`api/openapi.yaml`](api/openapi.yaml) | OpenAPI 3.0 description of the public HTTP API. |
| [`events/envelope.schema.json`](events/envelope.schema.json) | JSON Schema for the outer event object (`event_id`, `run_id`, `type`, …, `data`). |
| [`types/*.schema.json`](types/) | JSON Schema for **`data` only**, one file per `type` enum value (e.g. `CARD_PICKED.schema.json`). |
| [`runfile/run.v9.schema.json`](runfile/run.v9.schema.json) | JSON Schema for completed **run file** JSON (StS2 EA shape; permissive until real exports are mapped). |
| [`examples/`](examples/) | Example payloads for validators and docs. |

Human-facing API notes: [`../docs/api.md`](../docs/api.md).

## Validation flow

1. Parse JSON and validate the **envelope** with `events/envelope.schema.json`.
2. Branch on `type` and validate **`data`** with the matching `types/<TYPE>.schema.json`.
3. For `POST /imports/run-file`, validate the body with `runfile/run.v9.schema.json`.

## `additionalProperties`

- **Envelope:** `additionalProperties: false` (strict wire shape).
- **Per-type `data` and run file:** `additionalProperties: true` so the game can add fields during Early Access without breaking clients that only validate known keys.

## Changelog rule

**Any change under `contracts/` must add an entry to [`CHANGELOG.md`](CHANGELOG.md)** (version or `Unreleased`). This includes OpenAPI, schemas, and examples.

## JSON Schema dialect

Schemas use **JSON Schema draft-07** (`$schema: http://json-schema.org/draft-07/schema#`) for broad tooling support. Optional `format` keywords may be omitted where validators do not ship `date-time` / `uuid` format plugins.
