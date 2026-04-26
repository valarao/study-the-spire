#!/usr/bin/env bash
set -euo pipefail

PROJECT_ID="${PROJECT_ID:-study-the-spire}"
REGION="${REGION:-us-central1}"
ARTIFACT_REPO="${ARTIFACT_REPO:-study-the-spire}"
SERVICE_NAME="${SERVICE_NAME:-study-the-spire-api}"
TAG="${TAG:-$(date +%Y%m%d-%H%M%S)}"

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

echo "Building and pushing image with Cloud Build: ${IMAGE_URI}"
gcloud builds submit "${BACKEND_DIR}" --tag "${IMAGE_URI}" --ignore-file "${BACKEND_DIR}/.gcloudignore"

echo "Deploying Cloud Run service ${SERVICE_NAME}..."
gcloud run deploy "${SERVICE_NAME}" \
  --project "${PROJECT_ID}" \
  --region "${REGION}" \
  --platform managed \
  --image "${IMAGE_URI}" \
  --allow-unauthenticated \
  --set-env-vars "CONFIG=production"

SERVICE_URL="$(gcloud run services describe "${SERVICE_NAME}" --project "${PROJECT_ID}" --region "${REGION}" --format='value(status.url)')"

echo
echo "Deployed URL: ${SERVICE_URL}"
echo "Verify: curl ${SERVICE_URL}/hello"
