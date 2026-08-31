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
TWILIO_WHATSAPP_FROM=+14155238886
TWILIO_WHATSAPP_OTP_CONTENT_SID=HX...
```

### Créer le template OTP (Twilio Content API)

Script one-shot (Windows PowerShell) :

```powershell
$env:TWILIO_ACCOUNT_SID = "AC..."
$env:TWILIO_AUTH_TOKEN = "..."
.\deploy\scripts\create-twilio-whatsapp-otp-template.ps1
```

Le script crée un template `whatsapp/authentication` (catégorie Meta **AUTHENTICATION**) et affiche le `ContentSid` (`HX…`) à copier dans `TWILIO_WHATSAPP_OTP_CONTENT_SID`.

Référence Twilio : [whatsapp/authentication](https://www.twilio.com/docs/content/whatsappauthentication)

## Envoi

- Texte session 24h : `body`
- Template Meta via Twilio Content : `templateName` = ContentSid (`HX…`) + `templateData`
- **OTP WhatsApp (manuel)** : `messageType=OTP` + `otpCode` — voir ci-dessous
- **OTP intégré (recommandé)** : `POST /v1/otp/send` + `POST /v1/otp/verify` — voir [OTP.md](OTP.md)

### OTP intégré (génération hub)

```bash
curl -s -X POST http://localhost:8088/v1/otp/send \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{"to": "+22890909090", "channel": "WHATSAPP"}'
```

### OTP WhatsApp (code fourni par le client)

```bash
curl -s -X POST http://localhost:8088/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{
    "channel": "WHATSAPP",
    "messageType": "OTP",
    "otpCode": "424242",
    "to": ["+22890909090"]
  }'
```

`from` et `templateName` sont optionnels si `TWILIO_WHATSAPP_FROM` et `TWILIO_WHATSAPP_OTP_CONTENT_SID` sont configurés.

Erreurs de configuration hub :

| Code | HTTP | Cause |
|------|------|-------|
| `OTP_NOT_CONFIGURED` | 422 | ContentSid ou `from` manquant |
| `CHANNEL_NOT_ENABLED` | 422 | `WHATSAPP_ENABLED=false` |

### Sandbox vs production

| Phase | `TWILIO_WHATSAPP_FROM` | Notes |
|-------|------------------------|-------|
| Sandbox | `+14155238886` | Destinataires doivent rejoindre le sandbox Twilio |
| Production | Votre numéro WhatsApp Business | Enregistrer le sender dans Twilio Console |

## Ops P2

- `POST /v1/webhooks/twilio` — StatusCallback
- `POST /v1/notifications/{id}/replay` — `{ "mode": "DRY_RUN" | "RESEND" }`

## Client Java (starter)

```java
notificationHubClient.send(CreateNotificationRequest.whatsappOtp("+22890909090", "424242"));
```

Le hub doit avoir `TWILIO_WHATSAPP_FROM` et `TWILIO_WHATSAPP_OTP_CONTENT_SID` configurés côté serveur.
