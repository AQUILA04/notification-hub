# P2 — WhatsApp via Twilio (SMS = AfrikSMS)

## Stratégie

| Canal | Provider | Activé par défaut |
|-------|----------|-------------------|
| SMS | AfrikSMS | oui |
| WhatsApp | Twilio | **non** (`WHATSAPP_ENABLED=false`) |
| EMAIL | SMTP | oui |

## Canal désactivé

Si un client appelle `POST /v1/notifications` avec `"channel": "WHATSAPP"` alors que
`notification-hub.channels.whatsapp=false` :

- HTTP **422 Unprocessable Entity**
- `title`: Canal non disponible
- `code`: `CHANNEL_NOT_ENABLED`
- `channel`: `WHATSAPP`
- Message FR explicite : le hub n’utilise pas ce canal pour le moment

Découverte : `GET /v1/channels` → `{ "EMAIL": true, "SMS": true, "WHATSAPP": false }`

## Activer WhatsApp

```bash
WHATSAPP_ENABLED=true
WHATSAPP_PROVIDER=twilio
TWILIO_ACCOUNT_SID=...
TWILIO_AUTH_TOKEN=...
TWILIO_STATUS_CALLBACK_URL=https://hub.example.com/v1/webhooks/twilio
```

## Envoi

- Texte session 24h : `body`
- Template Meta via Twilio Content : `templateName` = ContentSid (`HX…`) + `templateData`

## Ops P2

- `POST /v1/webhooks/twilio` — StatusCallback
- `POST /v1/notifications/{id}/replay` — `{ "mode": "DRY_RUN" | "RESEND" }`
