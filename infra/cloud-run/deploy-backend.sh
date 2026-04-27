#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-study-the-spire}"
REGION="${REGION:-us-central1}"
ARTIFACT_REPO="${ARTIFACT_REPO:-study-the-spire}"
SERVICE_NAME="${SERVICE_NAME:-study-the-spire-api}"
TAG="${TAG:-$(date +%Y%m%d-%H%M%S)}"

CLOUD_SQL_INSTANCE="${CLOUD_SQL_INSTANCE:-study-the-spire-db}"
INSTANCE_CONNECTION_NAME="${INSTANCE_CONNECTION_NAME:-${PROJECT_ID}:${REGION}:${CLOUD_SQL_INSTANCE}}"
DB_NAME="${DB_NAME:-study_the_spire}"
DB_USER="${DB_USER:-app_user}"
DB_PASSWORD_SECRET="${DB_PASSWORD_SECRET:-stsa-db-password}"
CLERK_JWKS_URL_SECRET="${CLERK_JWKS_URL_SECRET:-CLERK_JWKS_URL}"
CLERK_ISSUER_SECRET="${CLERK_ISSUER_SECRET:-CLERK_ISSUER}"
SERVICE_ACCOUNT="${SERVICE_ACCOUNT:-${SERVICE_NAME}@${PROJECT_ID}.iam.gserviceaccount.com}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
BACKEND_DIR="${REPO_ROOT}/backend"
IMAGE_URI="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REPO}/${SERVICE_NAME}:${TAG}"

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "error: required command not found: $1" >&2
    exit 1
  }
}

require_cmd gcloud
require_cmd bash

echo "Using: project=${PROJECT_ID}, region=${REGION}, repo=${ARTIFACT_REPO}, service=${SERVICE_NAME}, tag=${TAG}"
echo "Cloud SQL: instance=${INSTANCE_CONNECTION_NAME}, db=${DB_NAME}, user=${DB_USER}, secret=${DB_PASSWORD_SECRET}"
echo "Clerk secrets: jwks=${CLERK_JWKS_URL_SECRET}, issuer=${CLERK_ISSUER_SECRET}"
echo "Service account: ${SERVICE_ACCOUNT}"

gcloud config set project "${PROJECT_ID}" >/dev/null

if ! gcloud artifacts repositories describe "${ARTIFACT_REPO}" --location "${REGION}" >/dev/null 2>&1; then
  echo "Creating Artifact Registry repository ${ARTIFACT_REPO} (${REGION})..."
  gcloud artifacts repositories create "${ARTIFACT_REPO}" \
    --repository-format=docker \
    --location="${REGION}" \
    --description="Study the Spire backend images"
fi

echo "Building backend JAR locally..."
(
  cd "${BACKEND_DIR}"
  ./gradlew clean installDist --no-daemon
)

if [ ! -x "${BACKEND_DIR}/build/install/study-the-spire-api/bin/study-the-spire-api" ]; then
  echo "error: application distribution not found in ${BACKEND_DIR}/build/install/study-the-spire-api" >&2
  exit 1
fi

echo "Ensuring service account has Secret Accessor access to Clerk secrets..."
for secret in "${CLERK_JWKS_URL_SECRET}" "${CLERK_ISSUER_SECRET}"; do
  gcloud secrets add-iam-policy-binding "${secret}" \
    --project "${PROJECT_ID}" \
    --member "serviceAccount:${SERVICE_ACCOUNT}" \
    --role "roles/secretmanager.secretAccessor" \
    --condition None \
    >/dev/null 2>&1 || true
done

echo "Building and pushing image with Cloud Build: ${IMAGE_URI}"
gcloud builds submit "${BACKEND_DIR}" --tag "${IMAGE_URI}" --ignore-file "${BACKEND_DIR}/.gcloudignore"

echo "Deploying Cloud Run service ${SERVICE_NAME}..."
gcloud run deploy "${SERVICE_NAME}" \
  --project "${PROJECT_ID}" \
  --region "${REGION}" \
  --platform managed \
  --image "${IMAGE_URI}" \
  --allow-unauthenticated \
  --service-account "${SERVICE_ACCOUNT}" \
  --add-cloudsql-instances "${INSTANCE_CONNECTION_NAME}" \
  --set-env-vars "CONFIG=production,INSTANCE_CONNECTION_NAME=${INSTANCE_CONNECTION_NAME},DB_NAME=${DB_NAME},DB_USER=${DB_USER}" \
  --set-secrets "DB_PASSWORD=${DB_PASSWORD_SECRET}:latest,CLERK_JWKS_URL=${CLERK_JWKS_URL_SECRET}:latest,CLERK_ISSUER=${CLERK_ISSUER_SECRET}:latest"

SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" --project "${PROJECT_ID}" --region "${REGION}" --format='value(status.url)')"

echo
echo "Deployed URL: ${SERVICE_URL}"
echo "Verify:"
echo "  curl ${SERVICE_URL}/hello"
echo "  curl ${SERVICE_URL}/db/ping"
