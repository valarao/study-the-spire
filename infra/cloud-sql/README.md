# Cloud SQL setup (backend)

One-time provisioning for the Postgres instance that backs `study-the-spire-api` on Cloud Run. The deploy script in [`../cloud-run/deploy-backend.sh`](../cloud-run/deploy-backend.sh) assumes the resources documented here already exist.

## Defaults

| Variable                  | Value                                                      |
|---------------------------|------------------------------------------------------------|
| `PROJECT_ID`              | `study-the-spire`                                          |
| `REGION`                  | `us-central1`                                              |
| Cloud SQL instance        | `study-the-spire-db`                                       |
| `INSTANCE_CONNECTION_NAME`| `study-the-spire:us-central1:study-the-spire-db`           |
| Database                  | `study_the_spire`                                          |
| App user                  | `app_user`                                                 |
| Secret Manager secret     | `stsa-db-password`                                         |
| Service account           | `study-the-spire-api@study-the-spire.iam.gserviceaccount.com` |

## Prerequisites

- `gcloud` installed and authenticated (`gcloud auth login`, `gcloud auth application-default login`).
- IAM permissions for Cloud SQL Admin, Secret Manager Admin, IAM Admin, and Cloud Run Admin on the project.

## One-time API enablement

```bash
gcloud services enable sqladmin.googleapis.com secretmanager.googleapis.com \
  --project study-the-spire
```

## Create the Postgres instance

```bash
gcloud sql instances create study-the-spire-db \
  --project study-the-spire \
  --database-version=POSTGRES_16 \
  --region=us-central1 \
  --tier=db-f1-micro \
  --edition=enterprise \
  --storage-auto-increase
```

The instance connection name follows the pattern `<project>:<region>:<instance>`. For the defaults above:

```text
INSTANCE_CONNECTION_NAME=study-the-spire:us-central1:study-the-spire-db
```

You can also print it from gcloud:

```bash
gcloud sql instances describe study-the-spire-db \
  --project study-the-spire \
  --format='value(connectionName)'
```

## Create the database and app user

```bash
gcloud sql databases create study_the_spire \
  --project study-the-spire \
  --instance=study-the-spire-db

# Generate a strong password locally (do not commit). Example with openssl:
DB_PASSWORD="$(openssl rand -base64 32)"

gcloud sql users create app_user \
  --project study-the-spire \
  --instance=study-the-spire-db \
  --password="${DB_PASSWORD}"
```

## Store the password in Secret Manager

```bash
printf '%s' "${DB_PASSWORD}" | gcloud secrets create stsa-db-password \
  --project study-the-spire \
  --replication-policy=automatic \
  --data-file=-
```

To rotate later, add a new version:

```bash
printf '%s' "${NEW_DB_PASSWORD}" | gcloud secrets versions add stsa-db-password \
  --project study-the-spire \
  --data-file=-
```

After rotation, also update the Cloud SQL user (`gcloud sql users set-password app_user --instance=study-the-spire-db --password="${NEW_DB_PASSWORD}"`) and redeploy Cloud Run so it picks up `stsa-db-password:latest`.

## Dedicated Cloud Run service account

The Cloud Run service runs as a dedicated service account with only the IAM roles it needs.

```bash
gcloud iam service-accounts create study-the-spire-api \
  --project study-the-spire \
  --display-name="Study the Spire API (Cloud Run)"

SA="study-the-spire-api@study-the-spire.iam.gserviceaccount.com"

# Allow connecting to Cloud SQL via the in-VM proxy.
gcloud projects add-iam-policy-binding study-the-spire \
  --member="serviceAccount:${SA}" \
  --role="roles/cloudsql.client"

# Allow reading the DB password secret.
gcloud secrets add-iam-policy-binding stsa-db-password \
  --project study-the-spire \
  --member="serviceAccount:${SA}" \
  --role="roles/secretmanager.secretAccessor"
```

## Local connectivity smoke test (optional)

Use the Cloud SQL Auth Proxy to verify the instance and credentials from your laptop:

```bash
cloud-sql-proxy study-the-spire:us-central1:study-the-spire-db &
psql "host=127.0.0.1 port=5432 dbname=study_the_spire user=app_user password=${DB_PASSWORD}"
```

## Deploy

After this one-time setup, deploy via [`../cloud-run/deploy-backend.sh`](../cloud-run/deploy-backend.sh) — the script reads `INSTANCE_CONNECTION_NAME`, `DB_NAME`, `DB_USER`, `DB_PASSWORD_SECRET`, and `SERVICE_ACCOUNT` (with sensible defaults that match this document).
