# Module OTP

Le module OTP est intégré au Notification Hub — pas de microservice séparé. Il gère le cycle complet :

1. **Génération** du code (SecureRandom, longueur configurable)
2. **Stockage** hashé en Redis (TTL, tentatives, cooldown renvoi)
3. **Envoi** via le pipeline existant (`NotificationService` → outbox → WhatsApp/SMS)
4. **Vérification** par API dédiée

## API

### Envoyer un OTP

```http
POST /v1/otp/send
X-Tenant-Id: demo-tenant
Content-Type: application/json

{
  "to": "+22890909090",
  "channel": "WHATSAPP"
}
```

`channel` est optionnel — défaut : `OTP_DEFAULT_CHANNEL` (`WHATSAPP`).

Réponse **202** :

```json
{
  "sessionId": "uuid",
  "expiresAt": "2026-08-31T18:00:00Z",
  "notificationId": "uuid",
  "channel": "WHATSAPP"
}
```

Le code n'est **jamais** retourné dans la réponse API.

### Vérifier un OTP

```http
POST /v1/otp/verify
X-Tenant-Id: demo-tenant
Content-Type: application/json

{
  "to": "+22890909090",
  "code": "424242"
}
```

Réponse **200** :

```json
{ "valid": true, "reason": "VALID" }
```

| reason | Signification |
|--------|---------------|
| `VALID` | Code correct — session supprimée (usage unique) |
| `INVALID` | Code incorrect |
| `EXPIRED` | Session absente ou TTL dépassé |
| `MAX_ATTEMPTS` | Trop de tentatives — session invalidée |

`sessionId` optionnel sur verify pour lier explicitement à une session d'envoi.

## Configuration

```bash
OTP_ENABLED=true
OTP_LENGTH=6
OTP_TTL_SECONDS=300
OTP_MAX_VERIFY_ATTEMPTS=5
OTP_RESEND_COOLDOWN_SECONDS=60
OTP_DEFAULT_CHANNEL=WHATSAPP
OTP_SMS_BODY_TEMPLATE=Votre code de verification est {{code}}. Valide {{ttlMinutes}} minutes.

# WhatsApp (Twilio)
WHATSAPP_ENABLED=true
TWILIO_WHATSAPP_FROM=+14155238886
TWILIO_WHATSAPP_OTP_CONTENT_SID=HX...

# SMS (AfrikSMS)
SMS_PROVIDER=afriksms
SMS_DEFAULT_FROM=MyBrand
```

## Erreurs HTTP

| code | HTTP | Description |
|------|------|-------------|
| `OTP_RESEND_COOLDOWN` | 429 | Renvoi trop rapide |
| `OTP_NOT_CONFIGURED` | 422 | ContentSid / from WhatsApp manquant |
| `CHANNEL_NOT_ENABLED` | 422 | Canal désactivé |

## Package Java (module interne)

```
com.optimizesolux.notificationhub.otp
├── api/           OtpController, DTOs, exceptions
├── application/   OtpService, OtpCodeGenerator, OtpHasher
├── domain/        OtpSession, OtpVerifyReason
└── infrastructure/ RedisOtpStore
```

## Client Spring Boot

```java
OtpSendResponse sent = client.sendOtp(OtpSendRequest.whatsApp("+22890909090"));
OtpVerifyResponse ok = client.verifyOtp(OtpVerifyRequest.of("+22890909090", userInput));
```

## Rétrocompatibilité

`POST /v1/notifications` avec `messageType=OTP` + `otpCode` reste disponible si l'application génère elle-même le code (sans vérification hub).

Voir aussi : [WHATSAPP_TWILIO.md](WHATSAPP_TWILIO.md)
