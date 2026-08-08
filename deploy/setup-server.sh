#!/usr/bin/env bash
# =============================================================================
# setup-server.sh — One-time Contabo setup for Notification Hub
# Préfère shared-traefik (/opt/optimizesolux) déjà présent sur le VPS.
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
for net in traefik-public notification-hub-prod-internal; do
  if docker network inspect "$net" > /dev/null 2>&1; then
    echo "      Network '$net' already exists, skipping."
  else
    docker network create "$net"
    echo "      Network '$net' created."
  fi
done

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
_kc_admin_pass="${NH_KEYCLOAK_ADMIN_PASSWORD:-CHANGE_ME_keycloak_admin_password}"
_app_host="${NH_APP_HOSTNAME_PROD:-notification.optimizesolux.com}"
_api_host="${NH_API_HOSTNAME_PROD:-notification-api.optimizesolux.com}"
_kc_host="${NH_KEYCLOAK_HOSTNAME_PROD:-notification-auth.optimizesolux.com}"
_mail_host="${NH_MAIL_HOST:-smtp.resend.com}"
_mail_port="${NH_MAIL_PORT:-465}"
_mail_user="${NH_MAIL_USER:-resend}"
_mail_pass="${NH_MAIL_PASS:-CHANGE_ME_resend_api_key}"
_mail_from="${NH_MAIL_FROM:-Notification Hub <noreply@optimizesolux.com>}"
_artemis_pass="${NH_ARTEMIS_PASSWORD:-CHANGE_ME_artemis_password}"
_redis_pass="${NH_REDIS_PASSWORD:-}"

_db_pass_q="$(env_quote "$_db_pass")"
_kc_admin_pass_q="$(env_quote "$_kc_admin_pass")"
_mail_pass_q="$(env_quote "$_mail_pass")"
_mail_from_q="$(env_quote "$_mail_from")"
_artemis_pass_q="$(env_quote "$_artemis_pass")"
_redis_pass_q="$(env_quote "$_redis_pass")"

PROD_ENV="$ROOT/prod/.env"
if [[ ! -f "$PROD_ENV" ]]; then
  cat > "$PROD_ENV" << EOF
# =============================================================================
# Notification Hub PROD — $ROOT/prod/.env
# =============================================================================
DB_USER=${_db_user}
DB_PASSWORD=${_db_pass_q}
DB_NAME=${_db_name}

KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=${_kc_admin_pass_q}
KEYCLOAK_HOSTNAME=${_kc_host}
KEYCLOAK_IMAGE=quay.io/keycloak/keycloak:26.2.5
KEYCLOAK_REALM_PATH=/opt/notification-hub/deploy/keycloak/realm-notification-hub.json

APP_HOSTNAME=${_app_host}
API_HOSTNAME=${_api_host}
CORS_ORIGINS=https://${_app_host}
OIDC_ISSUER_URI=https://${_kc_host}/realms/notification-hub

ARTEMIS_USER=artemis
ARTEMIS_PASSWORD=${_artemis_pass_q}
REDIS_PASSWORD=${_redis_pass_q}

MAIL_HOST=${_mail_host}
MAIL_PORT=${_mail_port}
MAIL_USER=${_mail_user}
MAIL_PASS=${_mail_pass_q}
MAIL_FROM=${_mail_from_q}

SMS_PROVIDER=afriksms
WHATSAPP_ENABLED=false

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
echo "  ${_kc_host}"
echo "Verify secrets in $PROD_ENV then deploy via init.sh / CD."
