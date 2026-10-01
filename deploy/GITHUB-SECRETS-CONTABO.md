# Secrets GitHub + DNS — Notification Hub (OptimizeSolux Contabo)

Prérequis VPS : **shared-traefik** + **optimize-common-infra** (réseau `optimizesolux-common`,
Keycloak `auth.optimizesolux.com`, Redis, Artemis).

## 1. DNS Cloudflare (DNS only / nuage gris)

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| A | `notification` | `169.58.127.90` | DNS only |
| A | `notification-api` | `169.58.127.90` | DNS only |

Auth partagée : `auth.optimizesolux.com` (common-infra) — **pas** de `notification-auth`.

## 2. Secrets repo (Actions) + environment `prod`

Réutilise la même clé SSH que SharedTraefik / CleanTrack si possible.

| Secret | Valeur |
|--------|--------|
| `SSH_PRIVATE_KEY` | contenu de `~\.ssh\optimizesolux_vps_ed25519` |
| `PROD_SERVER_HOST` | `169.58.127.90` |
| `PROD_SERVER_USER` | `root` |
| `GHCR_USERNAME` | user GitHub |
| `GHCR_TOKEN` | PAT `read:packages` (+ `write:packages` pour CI) |
| `DB_USER` | ex. `nhub` |
| `PROD_DB_PASSWORD` | mot de passe fort (Postgres métier) |
| `PROD_DB_NAME` | `notification_hub` |
| `PROD_APP_HOSTNAME` | `notification.optimizesolux.com` |
| `PROD_API_HOSTNAME` | `notification-api.optimizesolux.com` |
| `PROD_MAIL_HOST` | `smtp.resend.com` |
| `PROD_MAIL_PORT` | `465` |
| `PROD_MAIL_USER` | `resend` |
| `PROD_MAIL_PASS` | clé API Resend (`re_…`) |
| `PROD_MAIL_FROM` | `Notification Hub <noreply@optimizesolux.com>` |
| `PROD_ARTEMIS_PASSWORD` | **même** valeur que common-infra `.env` |
| `PROD_REDIS_PASSWORD` | **même** valeur que common-infra `.env` |
| `PROD_OTP_ENABLED` | `true` |
| `PROD_OTP_PROVIDER` | `twilio-verify` |
| `PROD_OTP_DEFAULT_CHANNEL` | `SMS` (ou `WHATSAPP` quand sender prod prêt) |
| `PROD_TWILIO_ACCOUNT_SID` | `AC…` (Console Twilio → Account Info) |
| `PROD_TWILIO_AUTH_TOKEN` | Auth Token Twilio (Show dans Console) |
| `PROD_TWILIO_VERIFY_SERVICE_SID` | `VA…` (Console → Verify → Services) |
| `PROD_WHATSAPP_TOKEN` | Meta System User token (si `WHATSAPP_PROVIDER=meta`) |
| `PROD_WHATSAPP_APP_SECRET` | Meta App Secret (HMAC webhook) |

Le CD injecte ces secrets dans `/opt/notification-hub/prod/.env` à chaque déploiement (`CT_UPDATE_ENV_SECRETS=true`).
`TWILIO_STATUS_CALLBACK_URL` est dérivé automatiquement : `https://<PROD_API_HOSTNAME>/v1/webhooks/twilio`.
Webhook Meta : `https://<PROD_API_HOSTNAME>/v1/webhooks/meta` — voir [backend/docs/WHATSAPP_META.md](../backend/docs/WHATSAPP_META.md).

SMS `environment=test` : même SMTP Resend, destinataires = **liste de diffusion SMS test** (cockpit → **SMS test** ; défaut seed : **sms@optimizesolux.com**, **ahonsueric01@gmail.com** ; repli `SMS_TEST_MAIL_TO` si liste vide — pas de second SMTP / Mailpit produit).

Créer aussi l’**environment** GitHub Actions nommé `prod` (approvals optionnels).

Obsolètes (ne plus utiliser) : `PROD_KEYCLOAK_ADMIN_PASSWORD`, `PROD_KEYCLOAK_HOSTNAME`.

## 3. Hosts runtime

| URL | Rôle |
|-----|------|
| https://notification.optimizesolux.com | Cockpit Angular |
| https://notification-api.optimizesolux.com | API Spring Boot |
| https://auth.optimizesolux.com/realms/notification-hub | Keycloak (common-infra) |

## 4. Pipelines

| Workflow | Trigger |
|----------|---------|
| **CI** | push / PR sur `main` et `release/**` → tests + push images GHCR |
| **CD** | CI success sur `release/**` **ou** `workflow_dispatch` (promote) → SSH Contabo |

Promote : déploie les dernières images publiées depuis l’historique `main`.

## 5. Source de vérité

- Runtime Docker (compose, scripts) = `deploy/` du dépôt
- À chaque `init.sh`, `/opt/notification-hub/deploy/` est resynchronisé depuis GitHub
- Secrets hors git : `/opt/notification-hub/prod/.env`
- Template : `deploy/.env.prod.example`
- Outils partagés : `/opt/optimizesolux/common-infra/`
