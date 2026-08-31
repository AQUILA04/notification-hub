#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   deploy.sh [--force-update | -fu] <env> [frontend_image] [backend_image]
#   env = prod

if [ "$#" -lt 1 ]; then
  echo "Usage: $0 [--force-update | -fu] <env> [frontend_image] [backend_image]" >&2
  exit 2
fi

if [[ "$1" == "--force-update" || "$1" == "-fu" ]]; then
  echo ">>> [deploy] Force update requested. Updating deploy scripts from GitHub..."
  REPO="${NHUB_GITHUB_REPO:-AQUILA04/notification-hub}"
  curl -sSL "https://raw.githubusercontent.com/${REPO}/main/deploy/update-deploy.sh" | bash
  shift
  if [ "$#" -lt 1 ]; then
    echo "Error: Missing environment argument after --force-update." >&2
    exit 2
  fi
  echo ">>> [deploy] Re-executing updated deploy.sh..."
  exec /opt/notification-hub/deploy/deploy.sh "$@"
fi

ENV="$1"
FRONTEND_ARG="${2:-}"
BACKEND_ARG="${3:-}"

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker-compose.$ENV.yml"
STACK_DIR="/opt/notification-hub/$ENV"
ENV_FILE="$STACK_DIR/.env"
RELEASES_DIR="$STACK_DIR/releases"
PROJECT_NAME="notification-hub-$ENV"
mkdir -p "$RELEASES_DIR"

env_quote() {
  local val="$1"
  if [[ "$val" == *[$'\n\r']* ]]; then
    echo "Error: newline in env value for key" >&2
    return 1
  fi
  if [[ "$val" =~ ^[A-Za-z0-9._:/+-]+$ ]]; then
    printf '%s' "$val"
  else
    printf "'%s'" "${val//\'/\'\\\'\'}"
  fi
}

safe_source_env() {
  local file="$1"
  [[ -f "$file" ]] || return 0
  set +u
  set -a
  # shellcheck disable=SC1090
  source "$file"
  set +a
  set -u
}

set_env_var() {
  local key="$1"
  local val="$2"
  local file="$ENV_FILE"
  local stored line found=false
  stored="$(env_quote "$val")"
  local tmp
  tmp=$(mktemp)

  if [[ -f "$file" ]]; then
    while IFS= read -r line || [[ -n "$line" ]]; do
      if [[ "$line" == "${key}="* ]] && [[ "$found" == false ]]; then
        printf '%s=%s\n' "$key" "$stored"
        found=true
      else
        printf '%s\n' "$line"
      fi
    done < "$file" > "$tmp"
  fi

  if [[ "$found" == false ]]; then
    printf '%s=%s\n' "$key" "$stored" >> "$tmp"
  fi

  cat "$tmp" > "$file"
  rm -f "$tmp"
}

set_env_var_if_missing() {
  local key="$1"
  local val="$2"
  if ! grep -q -E "^${key}=" "$ENV_FILE" 2>/dev/null; then
    set_env_var "$key" "$val"
    echo "  + added missing $key"
  elif grep -q -E "^${key}=$" "$ENV_FILE" 2>/dev/null && [[ -n "$val" ]]; then
    set_env_var "$key" "$val"
    echo "  + filled empty $key"
  fi
}

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Error: compose file not found: $COMPOSE_FILE" >&2
  exit 1
fi

if [[ -f "$ENV_FILE" ]]; then
  safe_source_env "$ENV_FILE"
else
  echo "Warning: $ENV_FILE not found. Run setup-server.sh or init.sh first." >&2
fi

if [[ -n "$FRONTEND_ARG" ]]; then
  FRONTEND_IMAGE="$FRONTEND_ARG"
else
  FRONTEND_IMAGE="${FRONTEND_IMAGE:-}"
fi

if [[ -n "$BACKEND_ARG" ]]; then
  BACKEND_IMAGE="$BACKEND_ARG"
else
  BACKEND_IMAGE="${BACKEND_IMAGE:-}"
fi

if [[ -z "$FRONTEND_IMAGE" || -z "$BACKEND_IMAGE" ]]; then
  echo "Error: FRONTEND_IMAGE and BACKEND_IMAGE must be provided as args or in $ENV_FILE" >&2
  exit 1
fi

touch "$ENV_FILE"
chmod 600 "$ENV_FILE" || true

if [[ -n "$FRONTEND_ARG" ]]; then
  set_env_var "FRONTEND_IMAGE" "$FRONTEND_IMAGE"
fi
if [[ -n "$BACKEND_ARG" ]]; then
  set_env_var "BACKEND_IMAGE" "$BACKEND_IMAGE"
fi

echo "Ensuring $ENV_FILE has required keys..."
if [[ "$ENV" == "prod" ]]; then
  set_env_var_if_missing APP_HOSTNAME "notification.optimizesolux.com"
  set_env_var_if_missing API_HOSTNAME "notification-api.optimizesolux.com"
  set_env_var_if_missing OIDC_ISSUER_URI "https://auth.optimizesolux.com/realms/notification-hub"
  set_env_var_if_missing MAIL_HOST "smtp.resend.com"
  set_env_var_if_missing MAIL_PORT "465"
  set_env_var_if_missing MAIL_USER "resend"
  set_env_var_if_missing MAIL_FROM "Notification Hub <noreply@optimizesolux.com>"
  set_env_var_if_missing ARTEMIS_USER "artemis"
  set_env_var_if_missing REDIS_DATABASE "1"
fi

if [[ "${CT_UPDATE_ENV_SECRETS:-}" == "true" ]]; then
  echo "CT_UPDATE_ENV_SECRETS=true — applying secret overrides from init/CD..."
  [[ -n "${NH_API_HOSTNAME_PROD:-}" ]] && set_env_var API_HOSTNAME "$NH_API_HOSTNAME_PROD"
  [[ -n "${NH_APP_HOSTNAME_PROD:-}" ]] && set_env_var APP_HOSTNAME "$NH_APP_HOSTNAME_PROD"
  [[ -n "${NH_OIDC_ISSUER_URI:-}" ]] && set_env_var OIDC_ISSUER_URI "$NH_OIDC_ISSUER_URI"
  [[ -n "${NH_MAIL_HOST:-}" ]] && set_env_var MAIL_HOST "$NH_MAIL_HOST"
  [[ -n "${NH_MAIL_PORT:-}" ]] && set_env_var MAIL_PORT "$NH_MAIL_PORT"
  [[ -n "${NH_MAIL_USER:-}" ]] && set_env_var MAIL_USER "$NH_MAIL_USER"
  [[ -n "${NH_MAIL_PASS:-}" ]] && set_env_var MAIL_PASS "$NH_MAIL_PASS"
  [[ -n "${NH_MAIL_FROM:-}" ]] && set_env_var MAIL_FROM "$NH_MAIL_FROM"
  [[ -n "${NH_DB_USER:-}" ]] && set_env_var DB_USER "$NH_DB_USER"
  [[ -n "${NH_DB_PASSWORD:-}" ]] && set_env_var DB_PASSWORD "$NH_DB_PASSWORD"
  [[ -n "${NH_DB_NAME:-}" ]] && set_env_var DB_NAME "$NH_DB_NAME"
  [[ -n "${NH_ARTEMIS_PASSWORD:-}" ]] && set_env_var ARTEMIS_PASSWORD "$NH_ARTEMIS_PASSWORD"
  [[ -n "${NH_REDIS_PASSWORD:-}" ]] && set_env_var REDIS_PASSWORD "$NH_REDIS_PASSWORD"
  [[ -n "${NH_REDIS_DATABASE:-}" ]] && set_env_var REDIS_DATABASE "$NH_REDIS_DATABASE"
  [[ -n "${NH_OTP_ENABLED:-}" ]] && set_env_var OTP_ENABLED "$NH_OTP_ENABLED"
  [[ -n "${NH_OTP_PROVIDER:-}" ]] && set_env_var OTP_PROVIDER "$NH_OTP_PROVIDER"
  [[ -n "${NH_OTP_DEFAULT_CHANNEL:-}" ]] && set_env_var OTP_DEFAULT_CHANNEL "$NH_OTP_DEFAULT_CHANNEL"
  [[ -n "${NH_TWILIO_ACCOUNT_SID:-}" ]] && set_env_var TWILIO_ACCOUNT_SID "$NH_TWILIO_ACCOUNT_SID"
  [[ -n "${NH_TWILIO_AUTH_TOKEN:-}" ]] && set_env_var TWILIO_AUTH_TOKEN "$NH_TWILIO_AUTH_TOKEN"
  [[ -n "${NH_TWILIO_VERIFY_SERVICE_SID:-}" ]] && set_env_var TWILIO_VERIFY_SERVICE_SID "$NH_TWILIO_VERIFY_SERVICE_SID"
fi

if [[ "$ENV" == "prod" ]]; then
  safe_source_env "$ENV_FILE"
  set_env_var_if_missing CORS_ORIGINS "https://${APP_HOSTNAME}"
  set_env_var_if_missing OIDC_ISSUER_URI "https://auth.optimizesolux.com/realms/notification-hub"
  set_env_var_if_missing OTP_ENABLED "true"
  set_env_var_if_missing OTP_PROVIDER "twilio-verify"
  set_env_var_if_missing OTP_DEFAULT_CHANNEL "SMS"
  if [[ -n "${API_HOSTNAME:-}" ]]; then
    set_env_var_if_missing TWILIO_STATUS_CALLBACK_URL "https://${API_HOSTNAME}/v1/webhooks/twilio"
  fi
fi

TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
RELEASE_FILE="$RELEASES_DIR/${ENV}_${TIMESTAMP}.txt"

echo "DEPLOY: env=$ENV"
echo "Using compose file: $COMPOSE_FILE"
echo "Using env file:     $ENV_FILE"
echo "Saving release metadata to $RELEASE_FILE"
{
  echo "FRONTEND_IMAGE=$FRONTEND_IMAGE"
  echo "BACKEND_IMAGE=$BACKEND_IMAGE"
  echo "TIMESTAMP=$TIMESTAMP"
} > "$RELEASE_FILE"

echo "Pulling images..."
if [ -n "${GHCR_USERNAME:-}" ] && [ -n "${GHCR_TOKEN:-}" ]; then
  echo "Logging in to ghcr.io as $GHCR_USERNAME"
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
fi

docker compose \
  -f "$COMPOSE_FILE" \
  --project-name "$PROJECT_NAME" \
  --env-file "$ENV_FILE" \
  pull

echo "Starting services..."
docker compose \
  -f "$COMPOSE_FILE" \
  --project-name "$PROJECT_NAME" \
  --env-file "$ENV_FILE" \
  up -d

ln -sfn "$RELEASE_FILE" "$RELEASES_DIR/${ENV}_current.txt"

if [[ "$ENV" == "prod" ]]; then
  safe_source_env "$ENV_FILE"
  APP_URL="https://${APP_HOSTNAME:-notification.optimizesolux.com}"
  API_URL="https://${API_HOSTNAME:-notification-api.optimizesolux.com}"
  AUTH_URL="${OIDC_ISSUER_URI:-https://auth.optimizesolux.com/realms/notification-hub}"
  echo "HTTP smoke: app=$APP_URL api=$API_URL auth=$AUTH_URL"
  sleep 8
  curl -sk -o /dev/null -w "app=%{http_code}\n" "$APP_URL/" || echo "WARN: app HTTP check failed"
  curl -sk -o /dev/null -w "api=%{http_code}\n" "$API_URL/actuator/health" || true
  curl -sk -o /dev/null -w "auth=%{http_code}\n" "$AUTH_URL" || echo "WARN: auth HTTP check failed (common-infra Keycloak)"
fi

echo "Deployment finished."
cat "$RELEASE_FILE"
echo "Done"
