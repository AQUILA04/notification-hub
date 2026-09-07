# Stratégie providers canaux

## Décision produit

| Canal | Provider | Notes |
|-------|----------|--------|
| **SMS** | **Brevo** | API transactionnelle — choix prod actuel |
| **WhatsApp** | **Twilio** | Twilio sert **uniquement** WhatsApp |
| **EMAIL** | SMTP (SPI) | Resend en prod ; Brevo email possible plus tard |

AfrikSMS reste dans le SPI (`SMS_PROVIDER=afriksms`) si besoin.

Local sans clés : `SMS_PROVIDER=logging`.

## Config SMS Brevo

```yaml
notification-hub:
  sms:
    provider: brevo
    default-from: OptimizeSLX   # sender API ≤ 11 alphanum (pas un enregistrement Sender ID)
    brevo:
      api-key: ${BREVO_API_KEY}
      base-url: https://api.brevo.com/v3
      webhook-url: ${BREVO_SMS_WEBHOOK_URL:}  # optionnel — POST /v1/webhooks/brevo
```

API : `POST /v3/transactionalSMS/send`  
Body : `sender`, `recipient`, `content`, `type=transactional` (+ `webUrl` si configuré).  
Numéros normalisés : code pays **sans** `+` ni `00` (ex. `22890909090`).

Crédits prepaid requis (pack min. 100). Pas d’upgrade plan Starter obligatoire pour l’API SMS.

## Config SMS AfrikSMS (legacy)

```yaml
notification-hub:
  sms:
    provider: afriksms
    default-from: MyBrand
    afriksms:
      client-id: ${AFRIKSMS_CLIENT_ID}
      api-key: ${AFRIKSMS_API_KEY}
      base-url: https://api.afriksms.com/api/web/web_v1/outbounds
```

API : `GET …/send` (1 destinataire) · `POST …/send_multisms` (N).

## WhatsApp (P2)

Voir [WHATSAPP_TWILIO.md](WHATSAPP_TWILIO.md) — `TwilioWhatsAppProvider` uniquement.

## Ajouter un autre SMS

Nouvelle classe `SmsProvider` + `SMS_PROVIDER=…` — aucun changement dispatch.
