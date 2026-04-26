# Cloud Run deployment (backend)

Defaults used by the deployment script:

- `PROJECT_ID=study-the-spire`
- `REGION=us-central1`
- `ARTIFACT_REPO=study-the-spire`
- `SERVICE_NAME=study-the-spire-api`

## Prerequisites

- `gcloud` installed and authenticated:
  - `gcloud auth login`
  - `gcloud auth application-default login`
- IAM permissions for Cloud Build, Artifact Registry, and Cloud Run in project `study-the-spire`.

## One-time API enablement

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com --project study-the-spire
```

## Deploy

From repository root:

```bash
./infra/cloud-run/deploy-backend.sh
```

The script builds a local Gradle application distribution (`backend/build/install/study-the-spire-api`) first, then submits a minimal Cloud Build context via `backend/.gcloudignore`.

Override defaults if needed:

```bash
PROJECT_ID=study-the-spire REGION=us-central1 ARTIFACT_REPO=study-the-spire SERVICE_NAME=study-the-spire-api ./infra/cloud-run/deploy-backend.sh
```

## Verify

After deploy, the script prints the service URL. Validate:

```bash
curl https://<service-url>/hello
```

Expected response:

```json
{"message":"the spire awaits"}
```
