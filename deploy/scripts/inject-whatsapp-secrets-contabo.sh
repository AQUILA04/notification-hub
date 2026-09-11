#!/usr/bin/env bash
set -euo pipefail
SRC=/tmp/optimizesolux-whatsapp.env
ENV=/opt/notification-hub/prod/.env
sed -i 's/\r$//' "$SRC"
set_kv() {
  local key="$1" val="$2"
  python3 - "$ENV" "$key" "$val" <<'PY'
import sys
path, key, val = sys.argv[1], sys.argv[2], sys.argv[3]
lines = []
found = False
try:
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith(key + "=") and not found:
                lines.append(f"{key}={val}\n")
                found = True
            else:
                lines.append(line)
except FileNotFoundError:
    pass
if not found:
    lines.append(f"{key}={val}\n")
with open(path, "w", encoding="utf-8") as f:
    f.writelines(lines)
PY
}
set -a
# shellcheck disable=SC1090
source "$SRC"
set +a
set_kv WHATSAPP_TOKEN "$WHATSAPP_TOKEN"
if [ -n "${WHATSAPP_APP_SECRET:-}" ]; then
  set_kv WHATSAPP_APP_SECRET "$WHATSAPP_APP_SECRET"
fi
set_kv WHATSAPP_PHONE_NUMBER_ID "$WHATSAPP_PHONE_NUMBER_ID"
set_kv WHATSAPP_WABA_ID "$WHATSAPP_WABA_ID"
set_kv WHATSAPP_APP_ID "$WHATSAPP_APP_ID"
set_kv WHATSAPP_WEBHOOK_VERIFY_TOKEN "$WHATSAPP_WEBHOOK_VERIFY_TOKEN"
set_kv WHATSAPP_ENABLED true
set_kv WHATSAPP_PROVIDER meta
chmod 600 "$ENV"
rm -f "$SRC"
TOKEN_LEN=$(printf '%s' "$WHATSAPP_TOKEN" | wc -c)
TOKEN_SUFFIX=$(printf '%s' "$WHATSAPP_TOKEN" | tail -c 4)
echo "TOKEN_LEN=$TOKEN_LEN"
echo "TOKEN_SUFFIX=$TOKEN_SUFFIX"
echo "APP_SECRET_SET=$([ -n "${WHATSAPP_APP_SECRET:-}" ] && echo yes || echo no)"
echo "ENV_TOKEN_SET=$(grep -q '^WHATSAPP_TOKEN=.\+' "$ENV" && echo yes || echo no)"
echo "ENV_SECRET_SET=$(grep -q '^WHATSAPP_APP_SECRET=.\+' "$ENV" && echo yes || echo no)"
