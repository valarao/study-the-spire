# Development setup

- This repo is a **monorepo**: `backend/`, `web/`, `mod/`, `contracts/`, `infra/`, `tools/`.
- Start with the milestone roadmap in [`references/study-the-spire-build-plan.md`](../references/study-the-spire-build-plan.md).

## Backend (Kotlin / Kairo)

- **JDK 21** and access to the **Highbeam Kairo** artifacts (see [`backend/README.md`](../backend/README.md) for Artifact Registry auth).
- Run the API: `cd backend && CONFIG=development ./gradlew run`.

## Local Postgres

The backend talks to Postgres via R2DBC. For local development, use the docker-compose stack in [`infra/local/`](../infra/local/docker-compose.yml).

```bash
docker compose -f infra/local/docker-compose.yml up -d
```

This starts Postgres 16 on `localhost:5432` with database `study_the_spire` and user/password `postgres`/`postgres`, matching `backend/src/main/resources/config/development.conf`.

To stop and wipe the volume:

```bash
docker compose -f infra/local/docker-compose.yml down -v
```

## Web (Next.js)

The dashboard lives in [`web/`](../web/) (Next.js 16, App Router, TypeScript, Tailwind, shadcn/ui).

```bash
cd web
pnpm install
pnpm dev
```

See [`web/README.md`](../web/README.md) for layout and scripts, and [`docs/web-deploy.md`](web-deploy.md) for Vercel setup.
