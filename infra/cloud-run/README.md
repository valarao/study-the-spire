# Cloud Run deployment (backend)

Defaults used by the deployment script:

| Variable                  | Default                                                       |
|---------------------------|---------------------------------------------------------------|
| `PROJECT_ID`              | `study-the-spire`                                             |
| `REGION`                  | `us-central1`                                                 |
| `ARTIFACT_REPO`           | `study-the-spire`                                             |
| `SERVICE_NAME`            | `study-the-spire-api`                                         |
| `CLOUD_SQL_INSTANCE`      | `study-the-spire-db`                                          |
| `INSTANCE_CONNECTION_NAME`| `${PROJECT_ID}:${REGION}:${CLOUD_SQL_INSTANCE}`               |
| `DB_NAME`                 | `study_the_spire`                                             |
| `DB_USER`                 | `app_user`                                                    |
| `DB_PASSWORD_SECRET`      | `stsa-db-password`                                            |
| `SERVICE_ACCOUNT`         | `${SERVICE_NAME}@${PROJECT_ID}.iam.gserviceaccount.com`       |

## Prerequisites

- `gcloud` installed and authenticated:
  - `gcloud auth login`
  - `gcloud auth application-default login`
- IAM permissions for Cloud Build, Artifact Registry, and Cloud Run in project `study-the-spire`.
- Cloud SQL instance, database, app user, password secret, and service account already provisioned per [`../cloud-sql/README.md`](../cloud-sql/README.md).

## One-time API enablement

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com --project study-the-spire
```

(For Cloud SQL and Secret Manager APIs, see [`../cloud-sql/README.md`](../cloud-sql/README.md).)

## Deploy

From repository root:

```bash
./infra/cloud-run/deploy-backend.sh
```

The script builds a local Gradle application distribution (`backend/build/install/study-the-spire-api`) first, then submits a minimal Cloud Build context via `backend/.gcloudignore`. The deploy step attaches the Cloud SQL instance via `--add-cloudsql-instances`, runs as the dedicated service account, mounts `DB_NAME`/`DB_USER`/`INSTANCE_CONNECTION_NAME` as env vars, and exposes `DB_PASSWORD` from Secret Manager.

Override defaults if needed:

```bash
PROJECT_ID=study-the-spire REGION=us-central1 ARTIFACT_REPO=study-the-spire SERVICE_NAME=study-the-spire-api ./infra/cloud-run/deploy-backend.sh
```

## Verify

After deploy, the script prints the service URL. Validate the public hello and the database round-trip:

```bash
curl https://<service-url>/hello
curl https://<service-url>/db/ping
```

Expected responses:

```json
{"message":"the spire awaits"}
{"ok":true,"databaseTime":"2026-04-26T12:34:56Z"}
```
