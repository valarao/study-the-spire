# Agent instructions — Study the Spire

Study the Spire turns Slay the Spire 2 runs into reviewable lessons: a **thin C# mod** in `mod/` uploads data; **Kotlin + Kairo** in `backend/` owns auth, persistence, and analysis on **Cloud Run** with **Cloud SQL Postgres**; **Next.js** in `web/` on **Vercel** (with **Clerk** and the Vercel AI SDK) is the dashboard. **`contracts/`** is the wire boundary (OpenAPI, JSON Schemas, examples). **`infra/`** holds deploy and local stack glue; **`docs/`** is human-facing documentation; **`tools/`** is for helpers such as a mock API.

## Where to look

- Milestones and deep design: `references/study-the-spire-build-plan.md` (may be gitignored locally).
- Onboarding as it grows: `docs/dev-setup.md`.
- After Milestone 1: `contracts/api/openapi.yaml` and `contracts/CHANGELOG.md`.

## Conventions

- Prefer **small, focused changes** that match existing style in the touched package.
- Follow **Implementation Principles** in the build plan (deploy early, thin mod, contracts as boundary, Clerk vs upload-token auth, user data scoped by `user_id`, etc.).
- **`contracts/`** is source of truth for wire formats. **Every contract change must update `contracts/CHANGELOG.md`** once that file exists.

## Stack keywords

C# (Slay the Spire 2 mod), Kotlin, Kairo, Gradle, Next.js, TypeScript, Postgres, Cloud Run, Cloud SQL, Vercel, Clerk, OpenAPI, JSON Schema.
