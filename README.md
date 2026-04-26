# Study the Spire

**Turn every run into a lesson.**

Study the Spire is a public **Slay the Spire 2** run tracker and analyzer: a thin **C# mod** uploads run data, a **Kotlin (Kairo) API** on Cloud Run is the source of truth with **Cloud SQL Postgres**, and a **Next.js** dashboard on Vercel (with Clerk) lets players review runs and stats.

## Repository layout

| Path | Role |
|------|------|
| `mod/` | C# game mod / exporter |
| `backend/` | Kotlin + Kairo API |
| `web/` | Next.js dashboard |
| `contracts/` | OpenAPI, event and run-file schemas, examples |
| `infra/` | Cloud Run, Cloud SQL, local Docker, deploy glue |
| `docs/` | Architecture, API, setup, and release docs |
| `tools/` | Helpers (e.g. mock backend) |

Agent-oriented notes: [`AGENTS.md`](AGENTS.md).
