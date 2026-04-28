# Changelog

All notable changes to this directory are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- `POST /imports/run-file` now documents `X-Local-Player-Id` header. The mod sends the uploader's SteamID64 so the importer can pick the right player out of co-op `players[]` arrays; absent header falls back to `players[0]` (single-player and pre-fix uploads).
- `GET /stats/summary` returning aggregate stats (totals, win rate, per-character + per-ascension breakdowns, top death causes) for the authenticated user.
- `Stats` tag for stats endpoints.
- `StatsSummaryResponse`, `CharacterStat`, `AscensionStat`, `DeathCauseStat` schemas.
- `GET /runs` query parameters: `character`, `ascension`, `status`, `from`, `to`, `cursor`, `limit`. Cursor-paginated; default page size 25, max 100.
- `RunsListResponse.nextCursor` field — opaque token for fetching the next page.
- `GET /runs` and `GET /runs/{runId}` for listing imported runs and fetching one with its original raw JSON (Clerk JWT auth).
- `Runs` tag for run listing/inspection endpoints.
- `RunRep`, `RunsListResponse`, `RunDetailResponse`, `ImportRunFileResponse` schemas.
- `POST /imports/run-file` now documents `X-Run-File-Name` header and returns `ImportRunFileResponse` (`imported`, `runId`). Idempotent on `(user, sha256)`.

### Changed

- `runfile/run.v9.schema.json` rewritten against real StS2 exports (kept locally under `references/run-samples/` — gitignored because they contain Steam IDs). Top-level required fields are now `build_id, schema_version, seed, start_time, was_abandoned, win, ascension, game_mode, platform_type, run_time`. The previous `run_id` and `status` (enum) fields are removed — the game does not emit them; the importer will derive a normalized status from `was_abandoned` + `win`. Nested shapes inside `acts`, `players`, and `map_point_history` remain unenforced for the EA window.
- `contracts/examples/runfile/sample_{win,loss,abandoned}.run.json` rebuilt as redacted minimal fixtures matching the new schema (no Steam IDs, minimal nested data).

### Added

- `GET /upload-tokens`, `POST /upload-tokens`, `DELETE /upload-tokens/{tokenId}` for managing per-user upload tokens (Clerk JWT auth). The raw secret is returned only on creation.
- `Tokens` tag for upload-token management endpoints.
- `UploadTokenRep`, `UploadTokensListResponse`, `CreateUploadTokenRequest`, `CreateUploadTokenResponse`, `DeleteUploadTokenResponse` schemas.
- `GET /me` endpoint (Clerk JWT auth) returning `userId` and `email`.
- `MeResponse` schema in `components/schemas`.
- `Account` tag for user identity endpoints.

### Changed

- Removed JSON Schema `$id` URIs from envelope, run file, and per-type schemas until a stable public identifier is chosen.

### Added

- Initial OpenAPI 3.0.3 spec at `api/openapi.yaml` (`/hello`, `/health`, `/db/ping`, `/mod/ping`, `/events`, `/events/batch`, `/imports/run-file`) with Clerk JWT and upload-token security schemes.
- Event envelope JSON Schema at `events/envelope.schema.json`.
- Per-event `data` JSON Schemas under `types/` for all eight event types.
- Example envelopes under `examples/events/` and example run files under `examples/runfile/`.
- First-pass run file JSON Schema `runfile/run.v9.schema.json` (permissive; EA exports may extend fields).
