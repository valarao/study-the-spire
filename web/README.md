# Study the Spire — Web

Next.js 16 dashboard for [Study the Spire](../README.md). App Router, TypeScript, Tailwind v4, shadcn/ui, Geist fonts, dark by default.

## Prerequisites

- Node.js 20+ (Next.js 16 requires Node 20.9+).
- pnpm 10+ (`corepack enable` or `brew install pnpm`).

## Develop

```bash
pnpm install
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000):

- `/` — landing hero.
- `/dashboard` — dashboard shell (sidebar + top nav + empty state).

## Build

```bash
pnpm build
pnpm start
```

## Layout

- `src/app/` — App Router routes (`/`, `/dashboard`).
- `src/components/` — shared components (`app-sidebar.tsx`, plus `ui/` shadcn primitives).
- `src/lib/utils.ts` — shadcn `cn()` helper.

## Environment

Copy `.env.example` to `.env.local`. `NEXT_PUBLIC_API_BASE_URL` is wired up in Milestone 6 once Clerk and the Kairo backend land.

## Deploy

See [../docs/web-deploy.md](../docs/web-deploy.md) for Vercel setup.
