# Notification Hub

Central multi-channel notification service (email, SMS, WhatsApp) for OptimizeSolux platforms.

One integration of providers; all apps call a single async API. Includes transactional outbox,
ActiveMQ Artemis queues, PostgreSQL Event Store, Pebble templates, retries, and multi-tenant
isolation via `tenant_id`.

## Stack (frozen)

| Layer | Choice |
|-------|--------|
| API | Spring Boot **3.5** · Java **25** |
| Cockpit | Angular **21** (P1) |
| IAM | Keycloak **26.*** |
| Broker | Apache ActiveMQ **Artemis** |
| Event Store | PostgreSQL append-only |
| Templates | **Pebble** |
| Cache / idempotency | Redis |
| Tenancy | Shared DB + `tenant_id` |
| Deploy | Docker Compose (default) · K8s manifests ready |

## Quick start (infra)

```bash
cp .env.example .env
docker compose up -d postgres redis artemis mailpit keycloak
```

| Service | URL / port |
|---------|------------|
| Postgres | `localhost:5433` |
| Redis | `localhost:6380` |
| Artemis | `tcp://localhost:61616` · console `:8161` |
| Mailpit UI | http://localhost:8025 |
| Keycloak | http://localhost:8081 |

Service accounts pour les apps appelantes :
[deploy/keycloak/SERVICE_ACCOUNTS.md](deploy/keycloak/SERVICE_ACCOUNTS.md)

Clients d’intégration (monorepo) :
- Maven Spring Boot starter : [`mvn/`](mvn/) (`sb-notification-hub-starter`)
- `npm/` · `gradle/` — prévus au même niveau

SPI providers (changer Brevo ↔ AfrikSMS / Twilio sans toucher au dispatch) :
[backend/docs/PROVIDERS.md](backend/docs/PROVIDERS.md)

WhatsApp via Twilio (P2) :
[backend/docs/WHATSAPP_TWILIO.md](backend/docs/WHATSAPP_TWILIO.md)

Module OTP intégré (génération + vérification) :
[backend/docs/OTP.md](backend/docs/OTP.md)

Guide d'intégration OTP pour les applications clientes :
[backend/docs/OTP_CLIENT_INTEGRATION.md](backend/docs/OTP_CLIENT_INTEGRATION.md)

## Run API locally (profile `local`)

Requires JDK **25** (Docker image uses 25; host may use a 25 toolchain).

```bash
cd backend
# Point to compose-mapped ports
set POSTGRES_PORT=5433
set REDIS_PORT=6380
set SMTP_PORT=1025
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

API: http://localhost:8088  
Health: http://localhost:8088/actuator/health  

With `local`, JWT is optional. Pass tenant header:

```http
X-Tenant-Id: demo-tenant
Idempotency-Key: optional-unique-key
```

### Send plain email

```bash
curl -s -X POST http://localhost:8088/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -H "Idempotency-Key: demo-1" \
  -d "{
    \"channel\": \"EMAIL\",
    \"from\": \"noreply@example.com\",
    \"to\": [\"user@example.com\"],
    \"subject\": \"Hello\",
    \"body\": \"<p>Plain body</p>\"
  }"
```

### Register template + send

```bash
curl -s -X POST http://localhost:8088/v1/templates \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d "{
    \"name\": \"order-ready-fr\",
    \"channel\": \"EMAIL\",
    \"subjectTemplate\": \"Commande {{ reference }} prête\",
    \"bodyTemplate\": \"<p>Bonjour {{ clientName }},</p>\"
  }"

curl -s -X POST http://localhost:8088/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d "{
    \"channel\": \"EMAIL\",
    \"from\": \"noreply@example.com\",
    \"to\": [\"user@example.com\"],
    \"templateName\": \"order-ready-fr\",
    \"templateData\": { \"clientName\": \"Awa\", \"reference\": \"CTP-1042\" }
  }"
```

Inspect Mailpit for delivered mail. Timeline:

`GET /v1/notifications/{id}/events`

### Send SMS (Brevo)

Stratégie : **SMS = Brevo** (transactionnel) · **WhatsApp = Twilio** (P2, pas de SMS Twilio).  
AfrikSMS reste disponible via `SMS_PROVIDER=afriksms`.

Le champ **`environment`** (`test` | `prod`) contrôle l’envoi réel :

| `environment` | Comportement SMS |
|---------------|------------------|
| omis / `test` / toute valeur ≠ `prod` | **Aucun SMS réel** — copie email à **sms@optimizesolux.com** via le SMTP du hub (Mailpit en local, **Resend** en prod) |
| `prod` | SMS réel via Brevo / AfrikSMS / provider configuré |

Défaut : **`test`**. En local, inspectez http://localhost:8025. En prod, ouvrez la boîte `sms@optimizesolux.com`.

```bash
# .env
SMS_PROVIDER=brevo
BREVO_API_KEY=xkeysib-...
SMS_DEFAULT_FROM=OptimizeSLX
# optionnel (prod) :
# BREVO_SMS_WEBHOOK_URL=https://notification-api.optimizesolux.com/v1/webhooks/brevo
# optionnel intercept test (SMTP du hub → sms@optimizesolux.com) :
# SMS_TEST_MAIL_FROM=noreply@optimizesolux.com
# SMS_TEST_MAIL_TO=sms@optimizesolux.com

# Test (défaut) — email de recette, pas de crédit SMS
curl -s -X POST http://localhost:8088/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d "{
    \"channel\": \"SMS\",
    \"environment\": \"test\",
    \"from\": \"OptimizeSLX\",
    \"to\": [\"+22890909090\"],
    \"body\": \"OTP 4242\"
  }"

# Prod — vrai SMS
curl -s -X POST http://localhost:8088/v1/notifications \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d "{
    \"channel\": \"SMS\",
    \"environment\": \"prod\",
    \"from\": \"OptimizeSLX\",
    \"to\": [\"+22890909090\"],
    \"body\": \"OTP 4242\"
  }"
```

Sans clés (dev) : `SMS_PROVIDER=logging`. Crédits prepaid Brevo requis pour un envoi **`prod`**.
Le starter Spring Boot injecte `environment` depuis le profil actif (`prod`/`production` → `prod`, sinon `test`).

### Send WhatsApp OTP (Twilio)

WhatsApp est désactivé par défaut (`WHATSAPP_ENABLED=false`). Voir [backend/docs/WHATSAPP_TWILIO.md](backend/docs/WHATSAPP_TWILIO.md).

**Recommandé — module OTP intégré** (génération + envoi + vérification) :

```bash
# .env — voir backend/docs/OTP.md
WHATSAPP_ENABLED=true
TWILIO_WHATSAPP_FROM=+14155238886
TWILIO_WHATSAPP_OTP_CONTENT_SID=HX...

# 1. Envoyer (le hub génère le code)
curl -s -X POST http://localhost:8088/v1/otp/send \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{"to": "+22890909090", "channel": "WHATSAPP"}'

# 2. Vérifier (code saisi par l'utilisateur)
curl -s -X POST http://localhost:8088/v1/otp/verify \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-tenant" \
  -d '{"to": "+22890909090", "code": "424242"}'
```

Alternative manuelle (code fourni par l'appelant) :

Créer le template Meta OTP :

```powershell
$env:TWILIO_ACCOUNT_SID = "AC..."
$env:TWILIO_AUTH_TOKEN = "..."
.\deploy\scripts\create-twilio-whatsapp-otp-template.ps1
```

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

### Admin KPI / list

```bash
curl -s "http://localhost:8088/v1/admin/kpi?windowHours=24" -H "X-Tenant-Id: demo-tenant"
curl -s "http://localhost:8088/v1/notifications?channel=SMS&page=0&size=20" -H "X-Tenant-Id: demo-tenant"
```

## Cockpit Angular (P1)

```bash
cd frontend
npm start
# http://localhost:4200 — set tenant in the sidebar (default demo-tenant)
```

Screens: Dashboard KPI (dont coût estimé/canal), Notifications, **DLQ** (requeue / discard), Templates.

Auth Keycloak : dans `src/environments/environment.ts`, passer
`auth.enabled: true` (realm `notification-hub`, client `notification-hub-console`).
En local, laisser `false` pour travailler avec `X-Tenant-Id` seul (API profile `local`).

Hardening P3 (quotas Redis, circuit breakers, audit) :
[backend/docs/HARDENING.md](backend/docs/HARDENING.md)

## CI / CD Contabo (shared-traefik + common-infra)

| Pipeline | Trigger | Action |
|----------|---------|--------|
| **CI** | push / PR sur `main` et `release/**` | tests + images GHCR (`*-frontend`, `*-backend`) |
| **CD** | CI OK sur `release/**` **ou** `workflow_dispatch` (promote) | SSH VPS → `init.sh` → compose + Traefik |

Prérequis VPS : `shared-traefik` + `optimize-common-infra`.

Docs secrets / DNS : [deploy/GITHUB-SECRETS-CONTABO.md](deploy/GITHUB-SECRETS-CONTABO.md)  
Scripts : [deploy/README.md](deploy/README.md)

Hosts cibles :

- https://notification.optimizesolux.com
- https://notification-api.optimizesolux.com
- https://auth.optimizesolux.com/realms/notification-hub (Keycloak partagé)

Local : `docker compose up -d postgres redis artemis mailpit keycloak` (stack autonome).
## Run API in Compose

```bash
# Build uses cert/ZscalerRootCA.pem (additional_contexts) so Maven Central works behind Zscaler
docker compose --profile app up -d --build api
```

Uses profile `local` inside the container for P0 smoke tests (no JWT). Switch to `docker`
only when Keycloak issuer is reachable and clients use bearer tokens.

If the image build fails with `PKIX path building failed`, confirm `cert/ZscalerRootCA.pem`
is present (copied from BCMS/cert or your corporate CA).

## Kubernetes

See [deploy/k8s/README.md](deploy/k8s/README.md). Manifests are prepared but **not** the
default Contabo path (Compose + shared-traefik via GitHub Actions CD).

## Layout

```
backend/                 Spring Boot API
frontend/                Angular cockpit
cert/                    ZscalerRootCA.pem (local Docker TLS / Maven Central)
deploy/                  Contabo Compose + Traefik scripts (init/deploy)
deploy/keycloak/         Realm import
deploy/k8s/              Optional cluster manifests
.github/workflows/       CI (main) + CD (release/** | promote)
docker-compose.yml       Local autonomous stack (PG + Redis + Artemis + Mailpit + Keycloak)
```

## P0 / P1 status

- [x] Schema Flyway + Event Store + outbox + templates
- [x] `POST/GET /v1/notifications` (+ events + list filtrée)
- [x] `POST/GET /v1/templates` (+ preview)
- [x] Artemis outbox → workers
- [x] EMAIL via SMTP (Mailpit) + SPI EmailProvider
- [x] Docker build behind Zscaler (`cert/ZscalerRootCA.pem`)
- [x] SMS adapter SPI — **Brevo** (prod) · AfrikSMS · logging (dev) · twilio/http optionnels
- [x] `GET /v1/admin/kpi`
- [x] Angular 21 cockpit (dashboard, notifs, templates)
- [x] Retry différé (`available_at` + priorité JMS + `_AMQ_SCHED_DELIVERY`)
- [x] Priorités HIGH/NORMAL/LOW sur outbox → JMS
- [x] Keycloak login cockpit (opt-in `auth.enabled`)
- [x] WhatsApp SPI Twilio + flag `WHATSAPP_ENABLED` (défaut off → 422 CHANNEL_NOT_ENABLED)
- [x] `GET /v1/channels` · webhook Twilio · replay DRY_RUN/RESEND
- [x] UI DLQ cockpit (`/dlq` · requeue / discard) + `GET/POST /v1/admin/dlq`
- [x] Quotas Redis / circuit breakers / audit tenant / KPI coût / K8s PDB+NetworkPolicy (P3)

Providers : [backend/docs/PROVIDERS.md](backend/docs/PROVIDERS.md) · WhatsApp P2 : [backend/docs/WHATSAPP_TWILIO.md](backend/docs/WHATSAPP_TWILIO.md) · P3 : [backend/docs/HARDENING.md](backend/docs/HARDENING.md)

## License

Proprietary — OptimizeSolux.
