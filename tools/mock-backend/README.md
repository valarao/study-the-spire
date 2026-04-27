# Mock backend

A tiny local server that fakes the four mod-facing backend endpoints. Use it
when iterating on the mod (or any other client) without depending on the real
Cloud Run deploy or a local Postgres.

## Run

```bash
node tools/mock-backend/server.mjs              # 127.0.0.1:8081
node tools/mock-backend/server.mjs --port 9090
node tools/mock-backend/server.mjs --host 0.0.0.0 --port 8081
```

No `pnpm install`, no dependencies — Node stdlib only.

## Point the mod at it

In your `<StS2>/mods/StudyTheSpire/config.ini`:

```ini
[study_the_spire]
upload_token = stsa_live_local_mock
endpoint = http://localhost:8081
enabled = true
```

Any token starting with `stsa_live_` is accepted. Launch StS2 and the mod's
ping should land in the mock's stdout.

## Endpoints

| Method | Path | Response |
|--------|------|----------|
| POST | `/mod/ping` | `{ "ok": true, "tokenName": "MockToken", "serverVersion": "mock-0.1.0" }` |
| POST | `/events` | `200`, no body |
| POST | `/events/batch` | `{ "accepted": N, "duplicates": 0, "rejected": [] }` |
| POST | `/imports/run-file` | `200`, no body |

All four require `Authorization: Bearer stsa_live_…`. Any other token or
missing header → `401`. Invalid JSON in the body → `400`. Anything else → `404`.

## What is not simulated

- Token revocation: `stsa_live_anything_at_all` works forever.
- `last_used_at` updates: nothing persists.
- Rate limiting: no `429`s, no `Retry-After`.
- Schema validation of bodies: any JSON is accepted.
- Server-side run analysis: payloads are logged-then-dropped.
- Multi-tenant identity: there's no `userId` resolved.

If your test depends on any of those behaviors, point the mod at the real
backend instead.

## Logging

Each request prints one line to stdout:

```
2026-04-27T05:12:33.456Z POST /mod/ping bytes=42 token=stsa_live_mock → 200
```

Token prefix is truncated to the first 14 characters, matching the dashboard's
display convention. Full tokens never appear in the log.
