# Contracts CI

Validates wire contracts against their schemas. Run locally before pushing
contract changes — the same script runs in `.github/workflows/contracts-ci.yml`.

## Run

```bash
cd tools/contracts-ci
pnpm install
pnpm validate
```

## What it checks

1. Every `contracts/**/*.schema.json` compiles as JSON Schema draft-07.
2. Every `contracts/examples/events/*.json` validates against
   `contracts/events/envelope.schema.json`, and its `data` validates against
   the matching `contracts/types/<TYPE>.schema.json`.
3. Every `contracts/examples/runfile/*.run.json` validates against
   `contracts/runfile/run.v9.schema.json`.
4. If `references/run-samples/` exists locally (gitignored — real exports with
   Steam IDs), each file in there also validates against the same schema.
5. `contracts/api/openapi.yaml` parses as a valid OpenAPI 3.0 document.

Exits non-zero on any failure with a clear `FAIL <file>: <reason>` line.

## Scope

This validator is a contract guard, not a content checker. It does **not**
verify that examples represent realistic data, that the schemas are tight
enough, or that nested fields inside `acts` / `players` make game-logic sense
— those concerns belong to the importer (M11+).
