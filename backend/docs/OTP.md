# Module OTP

> **Intégration côté applications clientes** (API, Spring Boot, curl, checklist) :
> [OTP_CLIENT_INTEGRATION.md](OTP_CLIENT_INTEGRATION.md)

Le module OTP est intégré au Notification Hub. Deux providers :

| Provider | Config | Usage |
|----------|--------|-------|
| **`twilio-verify`** (défaut) | `TWILIO_VERIFY_SERVICE_SID` | Twilio gère code + envoi + vérif — **recommandé** |
| **`internal`** | Redis + pipeline SMS/WhatsApp | Contrôle total, template Content requis pour WhatsApp |

## API (identique pour les deux providers)

### Envoyer

```http
POST /v1/otp/send
X-Tenant-Id: demo-tenant

{ "to": "+22890909090", "channel": "SMS" }
```

`environment` omis = **`test`** : le SMS OTP n’est **pas** envoyé par Twilio/Brevo — le hub génère le code (provider `internal`) et envoie une copie email à la **liste de diffusion SMS test** (défaut : **sms@optimizesolux.com**, **ahonsueric01@gmail.com**).  
Pour un vrai SMS : `{ "to": "+22890909090", "channel": "SMS", "environment": "prod" }`.

### Vérifier

```http
POST /v1/otp/verify
X-Tenant-Id: demo-tenant

{ "to": "+22890909090", "code": "424242" }
```

---

## Option A — Twilio Verify (recommandé)

Pas de template Meta à créer. Twilio gère le cycle OTP complet.

### 1. Créer un Verify Service (console web)

1. [console.twilio.com](https://console.twilio.com) → barre **Jump to…** → tapez **Verify**
2. Ou menu : **Explore products** → **Verify** → **Services**
3. **Create new Service**
4. **Friendly name** : `Notification Hub OTP`
5. **Create**
6. Copiez le **Service SID** (`VA…`) → `TWILIO_VERIFY_SERVICE_SID`

Lien direct : [Verify Services](https://console.twilio.com/us1/develop/verify/services)

### 2. Configuration `.env`

```bash
OTP_ENABLED=true
OTP_PROVIDER=twilio-verify
TWILIO_ACCOUNT_SID=AC...
TWILIO_AUTH_TOKEN=...
TWILIO_VERIFY_SERVICE_SID=VA...
OTP_DEFAULT_CHANNEL=SMS
```

**Compte Trial** : le numéro destinataire doit être **vérifié** dans  
[Verified Caller IDs](https://console.twilio.com/us1/develop/phone-numbers/manage/verified)

### 3. WhatsApp via Verify (production)

WhatsApp Verify nécessite un **WhatsApp Sender** enregistré (pas le sandbox Messaging).

```bash
OTP_DEFAULT_CHANNEL=WHATSAPP
OTP_TWILIO_VERIFY_WHATSAPP_SMS_FALLBACK=true   # fallback SMS auto
```

Twilio utilise un template Meta Authentication **fixe** — pas de ContentSid à gérer.

### 4. Tester

```bash
curl -X POST http://localhost:8088/v1/otp/send \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{"to": "+22890909090"}'

curl -X POST http://localhost:8088/v1/otp/verify \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{"to": "+22890909090", "code": "123456"}'
```

Réponse send inclut `provider: "twilio-verify"` et `providerReference: "VE…"`.

---

## Option B — Provider internal (Redis + hub)

```bash
OTP_PROVIDER=internal
OTP_DEFAULT_CHANNEL=SMS
SMS_PROVIDER=brevo
BREVO_API_KEY=
SMS_DEFAULT_FROM=OptimizeSLX
# Alternative: AfrikSMS → SMS_PROVIDER=afriksms + AFRIKSMS_*
```

WhatsApp internal nécessite `TWILIO_WHATSAPP_OTP_CONTENT_SID` (template Meta approuvé).

Voir [WHATSAPP_TWILIO.md](WHATSAPP_TWILIO.md).

---

## Erreurs HTTP

| code | HTTP | Description |
|------|------|-------------|
| `OTP_RESEND_COOLDOWN` | 429 | Provider internal — renvoi trop rapide |
| `OTP_NOT_CONFIGURED` | 422 | Provider internal WhatsApp mal configuré |
| — | 422 | `TWILIO_VERIFY_SERVICE_SID` manquant |

## Logs Twilio Verify

Console → **Verify** → **Logs** : tentatives, canal utilisé, erreurs.

## Client Java

```java
client.sendOtp(OtpSendRequest.of("+22890909090"));
client.verifyOtp(OtpVerifyRequest.of("+22890909090", code));
```
