# Stratégie providers canaux

## Décision produit (figée)

| Canal | Provider | Notes |
|-------|----------|--------|
| **SMS** | **AfrikSMS** | Moins cher que Twilio SMS — choix par défaut prod |
| **WhatsApp** | **Twilio** | Twilio sert **uniquement** WhatsApp au départ |
| **EMAIL** | SMTP (SPI) | Brevo possible plus tard via `EmailProvider` |

Ne pas activer Twilio pour le SMS tant que AfrikSMS couvre le besoin.

Local sans clés AfrikSMS : `SMS_PROVIDER=logging`.

## Config SMS AfrikSMS

```yaml
notification-hub:
  sms:
    provider: afriksms
    default-from: MyBrand   # SenderId ≤ 11 alphanum si `from` absent / invalide
    afriksms:
      client-id: ${AFRIKSMS_CLIENT_ID}
      api-key: ${AFRIKSMS_API_KEY}
      base-url: https://api.afriksms.com/api/web/web_v1/outbounds
```

API : `GET …/send` (1 destinataire) · `POST …/send_multisms` (N).  
Numéros normalisés : code pays **sans** `+` ni `00` (ex. `22890909090`).

## WhatsApp (P2)

Voir [WHATSAPP_TWILIO.md](WHATSAPP_TWILIO.md) — `TwilioWhatsAppProvider` uniquement.
SPI SMS reste AfrikSMS.

## Ajouter un autre SMS plus tard

Nouvelle classe `SmsProvider` + `SMS_PROVIDER=…` — aucun changement dispatch.
