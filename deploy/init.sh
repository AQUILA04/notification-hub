#!/usr/bin/env bash
# =============================================================================
# init.sh — Bootstrap Contabo Notification Hub (shared-traefik + common-infra)
# =============================================================================
# Usage (CD via SSH):
#   ./init.sh prod <frontend_image> <backend_image> [options...]
# =============================================================================
set -euo pipefail

INIT_DIR="$(cd "$(dirname "$0")" && pwd)"
DEPLOY_DIR="/opt/notification-hub/deploy"
GITHUB_REPO="${NHUB_GITHUB_REPO:-AQUILA04/notification-hub}"
GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"

ORIG_ARGS=("$@")

ENV=""
FRONTEND_IMAGE=""
BACKEND_IMAGE=""
FORCE_UPDATE=false

DB_USER=""
DB_PASSWORD=""
DB_NAME=""
APP_HOSTNAME_PROD=""
API_HOSTNAME_PROD=""
OIDC_ISSUER_URI=""
MAIL_HOST=""
MAIL_PORT=""
MAIL_USER=""
MAIL_PASS=""
MAIL_FROM=""
ARTEMIS_PASSWORD=""
REDIS_PASSWORD=""
REDIS_DATABASE=""
GHCR_USERNAME=""
GHCR_TOKEN=""
OTP_ENABLED=""
OTP_PROVIDER=""
OTP_DEFAULT_CHANNEL=""
TWILIO_ACCOUNT_SID=""
TWILIO_AUTH_TOKEN=""
TWILIO_VERIFY_SERVICE_SID=""

if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  ENV="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  FRONTEND_IMAGE="$1"; shift
fi
if [[ "$#" -ge 1 && "$1" != --* && "$1" != -* ]]; then
  BACKEND_IMAGE="$1"; shift
fi

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    --force-update|-fu) FORCE_UPDATE=true ;;
    --db-user)                    DB_USER="$2";                    shift ;;
    --db-password)                DB_PASSWORD="$2";                shift ;;
    --db-name)                    DB_NAME="$2";                    shift ;;
    --app-hostname-prod)          APP_HOSTNAME_PROD="$2";          shift ;;
    --api-hostname-prod)          API_HOSTNAME_PROD="$2";          shift ;;
    --oidc-issuer-uri)            OIDC_ISSUER_URI="$2";            shift ;;
    # Legacy aliases (ignored for Keycloak host/admin — auth is shared)
    --keycloak-admin-password)    shift ;; # discarded
    --keycloak-hostname-prod)     shift ;; # discarded
    --mail-host)                  MAIL_HOST="$2";                  shift ;;
    --mail-port)                  MAIL_PORT="$2";                  shift ;;
    --mail-user)                  MAIL_USER="$2";                  shift ;;
    --mail-pass)                  MAIL_PASS="$2";                  shift ;;
    --mail-from)                  MAIL_FROM="$2";                  shift ;;
    --artemis-password)           ARTEMIS_PASSWORD="$2";           shift ;;
    --redis-password)             REDIS_PASSWORD="$2";             shift ;;
    --redis-database)             REDIS_DATABASE="$2";             shift ;;
    --ghcr-username)              GHCR_USERNAME="$2";              shift ;;
    --ghcr-token)                 GHCR_TOKEN="$2";                 shift ;;
    --otp-enabled)                OTP_ENABLED="$2";                shift ;;
    --otp-provider)               OTP_PROVIDER="$2";               shift ;;
    --otp-default-channel)        OTP_DEFAULT_CHANNEL="$2";        shift ;;
    --twilio-account-sid)         TWILIO_ACCOUNT_SID="$2";         shift ;;
    --twilio-auth-token)          TWILIO_AUTH_TOKEN="$2";          shift ;;
    --twilio-verify-service-sid)  TWILIO_VERIFY_SERVICE_SID="$2";  shift ;;
    --github-repo)
      GITHUB_REPO="$2"
      export NHUB_GITHUB_REPO="$2"
      GITHUB_RAW="https://raw.githubusercontent.com/${GITHUB_REPO}/main/deploy"
      shift
      ;;
    *) echo "Unknown parameter: $1" >&2; exit 1 ;;
  esac
  shift
done

if [[ -z "$ENV" || -z "$FRONTEND_IMAGE" || -z "$BACKEND_IMAGE" ]]; then
  echo "Error: env, frontend_image and backend_image are required." >&2
  echo "Usage: $0 <env> <frontend_image> <backend_image> [options...]" >&2
  exit 1
fi

if [[ "${NHUB_INIT_SYNCED:-}" != "1" ]]; then
  echo ">>> [init] Syncing /opt/notification-hub/deploy from GitHub (repo is source of truth)..."
  mkdir -p /opt/notification-hub
  export NHUB_GITHUB_REPO="$GITHUB_REPO"
  bash <(curl -sSL "$GITHUB_RAW/update-deploy.sh")

  curl -sSL "$GITHUB_RAW/init.sh" -o /opt/notification-hub/init.sh
  chmod +x /opt/notification-hub/init.sh

  export NHUB_INIT_SYNCED=1
  echo ">>> [init] Re-executing synced init.sh from $DEPLOY_DIR..."
  exec "$DEPLOY_DIR/init.sh" "${ORIG_ARGS[@]}"
fi

if [[ "$FORCE_UPDATE" == "true" ]]; then
  echo ">>> [init] --force-update acknowledged (deploy/ already synced)."
fi

if [[ ! -d "$DEPLOY_DIR" ]]; then
  echo "Error: $DEPLOY_DIR missing after sync." >&2
  exit 1
fi

SETUP_MARKER="/opt/notification-hub/.server_initialized"

if [[ ! -f "$SETUP_MARKER" ]]; then
  echo ">>> [init] First-time setup detected. Running setup-server.sh..."

  export NH_DB_USER="${DB_USER:-nhub}"
  export NH_DB_PASSWORD="${DB_PASSWORD:-}"
  export NH_DB_NAME="${DB_NAME:-notification_hub}"
  export NH_APP_HOSTNAME_PROD="${APP_HOSTNAME_PROD:-notification.optimizesolux.com}"
  export NH_API_HOSTNAME_PROD="${API_HOSTNAME_PROD:-notification-api.optimizesolux.com}"
  export NH_OIDC_ISSUER_URI="${OIDC_ISSUER_URI:-https://auth.optimizesolux.com/realms/notification-hub}"
  export NH_MAIL_HOST="${MAIL_HOST:-smtp.resend.com}"
  export NH_MAIL_PORT="${MAIL_PORT:-465}"
  export NH_MAIL_USER="${MAIL_USER:-resend}"
  export NH_MAIL_PASS="${MAIL_PASS:-}"
  export NH_MAIL_FROM="${MAIL_FROM:-Notification Hub <noreply@optimizesolux.com>}"
  export NH_ARTEMIS_PASSWORD="${ARTEMIS_PASSWORD:-}"
  export NH_REDIS_PASSWORD="${REDIS_PASSWORD:-}"
  export NH_REDIS_DATABASE="${REDIS_DATABASE:-1}"
  export NH_OTP_ENABLED="${OTP_ENABLED:-true}"
  export NH_OTP_PROVIDER="${OTP_PROVIDER:-twilio-verify}"
  export NH_OTP_DEFAULT_CHANNEL="${OTP_DEFAULT_CHANNEL:-SMS}"
  export NH_TWILIO_ACCOUNT_SID="${TWILIO_ACCOUNT_SID:-}"
  export NH_TWILIO_AUTH_TOKEN="${TWILIO_AUTH_TOKEN:-}"
  export NH_TWILIO_VERIFY_SERVICE_SID="${TWILIO_VERIFY_SERVICE_SID:-}"

  bash "$DEPLOY_DIR/setup-server.sh"
  touch "$SETUP_MARKER"
  echo ">>> [init] Server setup complete. Marker written to $SETUP_MARKER"
else
  echo ">>> [init] Server already initialized (found $SETUP_MARKER). Skipping setup."
fi

echo ">>> [init] Launching deployment: env=$ENV"
export GHCR_USERNAME="${GHCR_USERNAME:-}"
export GHCR_TOKEN="${GHCR_TOKEN:-}"
export NHUB_GITHUB_REPO="$GITHUB_REPO"

# Allow CD secrets to refresh .env when explicitly needed
export CT_UPDATE_ENV_SECRETS="${CT_UPDATE_ENV_SECRETS:-true}"
export NH_DB_USER="${DB_USER:-}"
export NH_DB_PASSWORD="${DB_PASSWORD:-}"
export NH_DB_NAME="${DB_NAME:-}"
export NH_APP_HOSTNAME_PROD="${APP_HOSTNAME_PROD:-}"
export NH_API_HOSTNAME_PROD="${API_HOSTNAME_PROD:-}"
export NH_OIDC_ISSUER_URI="${OIDC_ISSUER_URI:-}"
export NH_MAIL_HOST="${MAIL_HOST:-}"
export NH_MAIL_PORT="${MAIL_PORT:-}"
export NH_MAIL_USER="${MAIL_USER:-}"
export NH_MAIL_PASS="${MAIL_PASS:-}"
export NH_MAIL_FROM="${MAIL_FROM:-}"
export NH_ARTEMIS_PASSWORD="${ARTEMIS_PASSWORD:-}"
export NH_REDIS_PASSWORD="${REDIS_PASSWORD:-}"
export NH_REDIS_DATABASE="${REDIS_DATABASE:-}"
export NH_OTP_ENABLED="${OTP_ENABLED:-true}"
export NH_OTP_PROVIDER="${OTP_PROVIDER:-twilio-verify}"
export NH_OTP_DEFAULT_CHANNEL="${OTP_DEFAULT_CHANNEL:-SMS}"
export NH_TWILIO_ACCOUNT_SID="${TWILIO_ACCOUNT_SID:-}"
export NH_TWILIO_AUTH_TOKEN="${TWILIO_AUTH_TOKEN:-}"
export NH_TWILIO_VERIFY_SERVICE_SID="${TWILIO_VERIFY_SERVICE_SID:-}"

bash "$DEPLOY_DIR/deploy.sh" "$ENV" "$FRONTEND_IMAGE" "$BACKEND_IMAGE"

echo ">>> [init] Done."
