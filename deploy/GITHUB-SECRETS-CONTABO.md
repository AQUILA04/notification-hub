# Secrets GitHub + DNS — Notification Hub (OptimizeSolux Contabo)

Après Shared Traefik déjà OK sur le VPS.

## 1. DNS Cloudflare (DNS only / nuage gris)

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| A | `notification` | `169.58.127.90` | DNS only |
| A | `notification-api` | `169.58.127.90` | DNS only |
| A | `notification-auth` | `169.58.127.90` | DNS only |

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
| `PROD_DB_PASSWORD` | mot de passe fort |
| `PROD_DB_NAME` | `notification_hub` |
| `PROD_KEYCLOAK_ADMIN_PASSWORD` | mot de passe fort |
| `PROD_APP_HOSTNAME` | `notification.optimizesolux.com` |
| `PROD_API_HOSTNAME` | `notification-api.optimizesolux.com` |
| `PROD_KEYCLOAK_HOSTNAME` | `notification-auth.optimizesolux.com` |
| `PROD_MAIL_HOST` | `smtp.resend.com` |
| `PROD_MAIL_PORT` | `465` |
| `PROD_MAIL_USER` | `resend` |
| `PROD_MAIL_PASS` | clé API Resend (`re_…`) |
| `PROD_MAIL_FROM` | `Notification Hub <noreply@optimizesolux.com>` |
| `PROD_ARTEMIS_PASSWORD` | mot de passe broker |
| `PROD_REDIS_PASSWORD` | optionnel (laisser vide OK) |

Créer aussi l’**environment** GitHub Actions nommé `prod` (approvals optionnels).

## 3. Hosts runtime

| URL | Rôle |
|-----|------|
| https://notification.optimizesolux.com | Cockpit Angular |
| https://notification-api.optimizesolux.com | API Spring Boot |
| https://notification-auth.optimizesolux.com | Keycloak |

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
