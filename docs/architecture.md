# Architecture

- High-level flow: **STS2 mod** → **Kairo API (Cloud Run)** → **Postgres (Cloud SQL)**; **Next.js (Vercel)** reads the API; users sign in with **Clerk**.
- See the architecture section in [`references/study-the-spire-build-plan.md`](../references/study-the-spire-build-plan.md) for the full diagram and component table.
- This document will be expanded as services land (Milestone 2+).
