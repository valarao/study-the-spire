# Web deploy (Vercel)

The Next.js dashboard in [`web/`](../web/) deploys to Vercel. Project name: `study-the-spire-web`.

## Prerequisites

- pnpm 10+ (`brew install pnpm` or `corepack enable`).
- Vercel CLI: `pnpm add -g vercel@latest`.
- A Vercel account with access to the team/project.
- Authenticate once: `vercel login`.

## First-time link

Link the local checkout to a Vercel project (run inside `web/`):

```bash
cd web
vercel link
```

Pick the team, then either select the existing `study-the-spire-web` project or create it. Vercel writes credentials to `web/.vercel/` (already gitignored).

## Environment variables

The dashboard reads `NEXT_PUBLIC_API_BASE_URL` (see [`web/.env.example`](../web/.env.example)). It is unused in Milestone 5 — Milestone 6 wires it up once the Kairo backend and Clerk are in place.

Add it to preview and production once the backend URL is known:

```bash
cd web
vercel env add NEXT_PUBLIC_API_BASE_URL preview
vercel env add NEXT_PUBLIC_API_BASE_URL production
```

For local dev, pull the env into `.env.local`:

```bash
vercel env pull .env.local
```

## Preview deploy

```bash
cd web
vercel
```

This builds and uploads a preview deployment, then prints the preview URL.

## Production deploy

```bash
cd web
vercel --prod
```

## Acceptance

- The Vercel preview URL renders the landing page (`Study the Spire` heading + tagline + "Open dashboard" link).
- `/dashboard` renders the sidebar, top breadcrumb, and the empty-state "No runs yet" card.

## Troubleshooting

- **`Command not found: vercel`** — install with `pnpm add -g vercel@latest`.
- **`No project linked`** — run `vercel link` again from inside `web/`.
- **Build fails on Vercel** — reproduce locally with `cd web && pnpm install && pnpm build`.
