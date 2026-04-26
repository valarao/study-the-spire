# Development setup

- This repo is a **monorepo**: `backend/`, `web/`, `mod/`, `contracts/`, `infra/`, `tools/`.
- Start with the milestone roadmap in [`references/study-the-spire-build-plan.md`](../references/study-the-spire-build-plan.md).

## Backend (Kotlin / Kairo)

- **JDK 21** and access to the **Highbeam Kairo** artifacts (see [`backend/README.md`](../backend/README.md) for Artifact Registry auth).
- Run the API: `cd backend && CONFIG=development ./gradlew run`.
