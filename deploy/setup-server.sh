#!/usr/bin/env bash
# =============================================================================
# setup-server.sh — One-time Contabo setup for Notification Hub
# Prérequis: shared-traefik + optimize-common-infra (optimizesolux-common).
# =============================================================================
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="/opt/notification-hub"

echo "=== Notification Hub Server Setup ==="

echo "[1/5] Checking Docker installation..."
if ! command -v docker &>/dev/null; then
  echo "      Docker not found. Installing..."
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg git
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
    | tee /etc/apt/sources.list.d/docker.list > /dev/null
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  echo "      Docker installed successfully."
else
  echo "      Docker already installed."
fi

echo "[2/5] Creating directory structure..."
mkdir -p "$ROOT/prod/releases"
mkdir -p "$ROOT/deploy"
if [[ "$DEPLOY_DIR" != "$ROOT/deploy" ]]; then
  cp -a "$DEPLOY_DIR"/. "$ROOT/deploy/"
  chmod +x "$ROOT/deploy"/*.sh 2>/dev/null || true
fi
echo "      Directories created."

echo "[3/5] Creating Docker networks..."
for net in traefik-public optimizesolux-common; do
  if docker network inspect "$net" > /dev/null 2>&1; then
    echo "      Network '$net' already exists, skipping."
  else
    docker network create "$net"
    echo "      Network '$net' created."
  fi
done

if ! docker ps --format '{{.Names}}' | grep -Eq 'oci-redis|redis'; then
  # Soft check — common-infra project name may vary; warn if compose project missing
  if [[ ! -d /opt/optimizesolux/common-infra ]]; then
    echo "      WARNING: /opt/optimizesolux/common-infra not found."
    echo "      Install optimize-common-infra before deploying this product."
  else
    echo "      Tip: ensure common-infra is up: sudo /opt/optimizesolux/common-infra/install.sh"
  fi
fi

env_quote() {
  local val="$1"
  if [[ "$val" =~ ^[A-Za-z0-9._:/+-]+$ ]]; then
    printf '%s' "$val"
  else
    printf "'%s'" "${val//\'/\'\\\'\'}"
  fi
}

echo "[4/5] Creating .env files..."

_db_user="${NH_DB_USER:-nhub}"
_db_pass="${NH_DB_PASSWORD:-CHANGE_ME_prod_db_password}"
_db_name="${NH_DB_NAME:-notification_hub}"
_app_host="${NH_APP_HOSTNAME_PROD:-notification.optimizesolux.com}"
_api_host="${NH_API_HOSTNAME_PROD:-notification-api.optimizesolux.com}"
_oidc="${NH_OIDC_ISSUER_URI:-https://auth.optimizesolux.com/realms/notification-hub}"
_mail_host="${NH_MAIL_HOST:-smtp.resend.com}"
_mail_port="${NH_MAIL_PORT:-465}"
_mail_user="${NH_MAIL_USER:-resend}"
_mail_pass="${NH_MAIL_PASS:-CHANGE_ME_resend_api_key}"
_mail_from="${NH_MAIL_FROM:-Notification Hub <noreply@optimizesolux.com>}"
_artemis_user="${NH_ARTEMIS_USER:-artemis}"
_artemis_pass="${NH_ARTEMIS_PASSWORD:-CHANGE_ME_artemis_password}"
_redis_pass="${NH_REDIS_PASSWORD:-CHANGE_ME_redis}"
_redis_db="${NH_REDIS_DATABASE:-1}"
_otp_enabled="${NH_OTP_ENABLED:-true}"
_otp_provider="${NH_OTP_PROVIDER:-twilio-verify}"
_otp_default_channel="${NH_OTP_DEFAULT_CHANNEL:-SMS}"
_twilio_account_sid="${NH_TWILIO_ACCOUNT_SID:-}"
_twilio_auth_token="${NH_TWILIO_AUTH_TOKEN:-}"
_twilio_verify_sid="${NH_TWILIO_VERIFY_SERVICE_SID:-}"

_db_pass_q="$(env_quote "$_db_pass")"
_mail_pass_q="$(env_quote "$_mail_pass")"
_mail_from_q="$(env_quote "$_mail_from")"
_artemis_pass_q="$(env_quote "$_artemis_pass")"
_redis_pass_q="$(env_quote "$_redis_pass")"
_twilio_account_sid_q="$(env_quote "$_twilio_account_sid")"
_twilio_auth_token_q="$(env_quote "$_twilio_auth_token")"
_twilio_verify_sid_q="$(env_quote "$_twilio_verify_sid")"

PROD_ENV="$ROOT/prod/.env"
if [[ ! -f "$PROD_ENV" ]]; then
  cat > "$PROD_ENV" << EOF
# =============================================================================
# Notification Hub PROD — $ROOT/prod/.env
# Shared tools: optimize-common-infra (redis/artemis/keycloak on optimizesolux-common)
# =============================================================================
DB_USER=${_db_user}
DB_PASSWORD=${_db_pass_q}
DB_NAME=${_db_name}

APP_HOSTNAME=${_app_host}
API_HOSTNAME=${_api_host}
CORS_ORIGINS=https://${_app_host}
OIDC_ISSUER_URI=${_oidc_q}

ARTEMIS_USER=${_artemis_user}
ARTEMIS_PASSWORD=${_artemis_pass_q}
REDIS_PASSWORD=${_redis_pass_q}
REDIS_DATABASE=${_redis_db}

MAIL_HOST=${_mail_host}
MAIL_PORT=${_mail_port}
MAIL_USER=${_mail_user}
MAIL_PASS=${_mail_pass_q}
MAIL_FROM=${_mail_from_q}

SMS_PROVIDER=afriksms
WHATSAPP_ENABLED=false

# OTP — Twilio Verify (provider twilio-verify)
OTP_ENABLED=${_otp_enabled}
OTP_PROVIDER=${_otp_provider}
OTP_DEFAULT_CHANNEL=${_otp_default_channel}
TWILIO_ACCOUNT_SID=${_twilio_account_sid_q}
TWILIO_AUTH_TOKEN=${_twilio_auth_token_q}
TWILIO_VERIFY_SERVICE_SID=${_twilio_verify_sid_q}
TWILIO_STATUS_CALLBACK_URL=https://${_api_host}/v1/webhooks/twilio

FRONTEND_IMAGE=
BACKEND_IMAGE=
EOF
  chmod 600 "$PROD_ENV"
  echo "      Created $PROD_ENV"
else
  echo "      $PROD_ENV already exists, skipping."
fi

echo "[5/5] Checking Traefik reverse proxy..."
if docker ps --format '{{.Names}}' | grep -qx 'shared-traefik'; then
  echo "      Shared Traefik (shared-traefik) already running — skipping local Traefik."
elif ss -tlnp 2>/dev/null | grep -q ':80 '; then
  echo "      Port 80 already in use — skipping local Traefik bootstrap."
else
  echo "      WARNING: shared-traefik not detected and :80 free."
  echo "      Install/start OptimizeSolux shared-traefik before exposing HTTPS."
fi

echo ""
echo "=== Setup complete! ==="
echo "DNS (grey cloud) → this server:"
echo "  ${_app_host}"
echo "  ${_api_host}"
echo "Auth (shared): ${_oidc}"
echo "Verify secrets in $PROD_ENV (REDIS/ARTEMIS must match common-infra .env)."
echo "Then deploy via init.sh / CD."
