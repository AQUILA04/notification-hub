# WhatsApp via Meta Cloud API

Alternative à Twilio pour le canal WhatsApp. SMS reste Brevo / AfrikSMS.

## Stratégie

| Canal | Provider | Activé par défaut |
|-------|----------|-------------------|
| SMS | Brevo | oui |
| WhatsApp | Meta Cloud API (`WHATSAPP_PROVIDER=meta`) | **non** (`WHATSAPP_ENABLED=false`) |
| EMAIL | SMTP | oui |

Twilio reste disponible : `WHATSAPP_PROVIDER=twilio` (voir [WHATSAPP_TWILIO.md](WHATSAPP_TWILIO.md)).

## Webhook Meta (obligatoire pour Verify)

| Champ Meta | Valeur |
|------------|--------|
| Callback URL | `https://notification-api.optimizesolux.com/v1/webhooks/meta` |
| Verify token | valeur de `WHATSAPP_WEBHOOK_VERIFY_TOKEN` (ex. `nhub-meta-wa-7f3c9e2a4b18`) |
| Subscribe | champ `messages` |

Endpoints hub :

- `GET /v1/webhooks/meta` — handshake (`hub.mode`, `hub.verify_token`, `hub.challenge`)
- `POST /v1/webhooks/meta` — statuts `sent` / `delivered` / `read` / `failed` (HMAC `X-Hub-Signature-256`)

Sans App Secret (`WHATSAPP_APP_SECRET`), le POST est accepté avec un warning log (dev seulement). **En prod, configurer l’App Secret.**

## Config

```bash
WHATSAPP_ENABLED=true
WHATSAPP_PROVIDER=meta
WHATSAPP_TOKEN=...                    # System User permanent (secret)
WHATSAPP_PHONE_NUMBER_ID=1361333320386192
WHATSAPP_WABA_ID=1429502245716883
WHATSAPP_APP_ID=1028507183525436
WHATSAPP_GRAPH_VERSION=v25.0
WHATSAPP_APP_SECRET=...               # App → Settings → Basic (secret)
WHATSAPP_WEBHOOK_VERIFY_TOKEN=nhub-meta-wa-7f3c9e2a4b18
WHATSAPP_OTP_TEMPLATE_NAME=otp_login  # template Authentication Meta approuvé
WHATSAPP_OTP_TEMPLATE_LANG=fr
WHATSAPP_OTP_BUTTON=copy_code
```

Secrets GitHub (optionnel, injectés au deploy) :

- `PROD_WHATSAPP_TOKEN`
- `PROD_WHATSAPP_APP_SECRET`

Ne jamais coller le token permanent dans le chat ou un ticket.

## Envoi

Graph : `POST https://graph.facebook.com/v25.0/{PHONE_NUMBER_ID}/messages`

### OTP manuel

```bash
curl -s -X POST https://notification-api.optimizesolux.com/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{
    "channel": "WHATSAPP",
    "messageType": "OTP",
    "otpCode": "424242",
    "to": ["+22892181351"]
  }'
```

`templateName` optionnel si `WHATSAPP_OTP_TEMPLATE_NAME` est défini. `from` optionnel (l’ID téléphone est dans l’URL Graph).

### Hello world (template Meta sans variables)

```bash
curl -s -X POST https://notification-api.optimizesolux.com/v1/notifications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{
    "channel": "WHATSAPP",
    "from": "meta",
    "to": ["+22892181351"],
    "templateName": "hello_world"
  }'
```

Destinataire test autorisé (sandbox / test number) : `+22892181351`.

## Checklist mise en service

1. Déployer le hub avec les endpoints webhook
2. Mettre token + App Secret dans `/opt/notification-hub/prod/.env`
3. Template Authentication Meta approuvé (ou `hello_world` pour le premier ping)
4. Dans Meta Developer → Webhooks : coller URL + verify token, s’abonner à `messages`
5. Smoke OTP / hello_world vers le numéro test
