#!/usr/bin/env bash
set -euo pipefail
ENV=/opt/notification-hub/prod/.env
set_kv() {
  local key="$1" val="$2"
  if grep -q "^${key}=" "$ENV" 2>/dev/null; then
    sed -i "s|^${key}=.*|${key}=${val}|" "$ENV"
  else
    printf '%s=%s\n' "$key" "$val" >> "$ENV"
  fi
}
set_kv WHATSAPP_ENABLED true
set_kv WHATSAPP_PROVIDER meta
set_kv WHATSAPP_PHONE_NUMBER_ID 1361333320386192
set_kv WHATSAPP_WABA_ID 1429502245716883
set_kv WHATSAPP_APP_ID 1028507183525436
set_kv WHATSAPP_GRAPH_VERSION v25.0
set_kv WHATSAPP_WEBHOOK_VERIFY_TOKEN nhub-meta-wa-7f3c9e2a4b18
set_kv WHATSAPP_OTP_TEMPLATE_LANG fr
set_kv WHATSAPP_OTP_BUTTON copy_code
grep -q '^WHATSAPP_TOKEN=' "$ENV" || echo 'WHATSAPP_TOKEN=' >> "$ENV"
grep -q '^WHATSAPP_APP_SECRET=' "$ENV" || echo 'WHATSAPP_APP_SECRET=' >> "$ENV"
grep -q '^WHATSAPP_OTP_TEMPLATE_NAME=' "$ENV" || echo 'WHATSAPP_OTP_TEMPLATE_NAME=' >> "$ENV"
echo 'ENV UPDATED'
grep -E '^(WHATSAPP_|TWILIO_WHATSAPP)' "$ENV" | sed -E 's/(TOKEN|SECRET)=.+/\1=***REDACTED***/'
