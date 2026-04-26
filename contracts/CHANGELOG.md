# Changelog

All notable changes to this directory are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- Removed JSON Schema `$id` URIs from envelope, run file, and per-type schemas until a stable public identifier is chosen.

### Added

- Initial OpenAPI 3.0.3 spec at `api/openapi.yaml` (`/hello`, `/health`, `/db/ping`, `/mod/ping`, `/events`, `/events/batch`, `/imports/run-file`) with Clerk JWT and upload-token security schemes.
- Event envelope JSON Schema at `events/envelope.schema.json`.
- Per-event `data` JSON Schemas under `types/` for all eight event types.
- Example envelopes under `examples/events/` and example run files under `examples/runfile/`.
- First-pass run file JSON Schema `runfile/run.v9.schema.json` (permissive; EA exports may extend fields).
