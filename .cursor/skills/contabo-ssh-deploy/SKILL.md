---
name: contabo-ssh-deploy
description: >-
  Déploie Notification Hub en production via SSH sur le VPS Contabo
  (shared-traefik). Use when the user asks to deploy, déployer, promote to prod,
  Contabo/VPS for notification-hub, or refuses the CD pipeline.
---

# Contabo SSH Deploy — Notification Hub

## Préférence

Préférer le **CD GitHub** (`release/**` ou `workflow_dispatch` promote).
SSH direct en secours (même pattern que CleanTrack).

Docs : `deploy/GITHUB-SECRETS-CONTABO.md`, `deploy/README.md`.

## Accès

| Paramètre | Valeur |
|-----------|--------|
| Host | `169.58.127.90` |
| User | `root` |
| Clé | `%USERPROFILE%\.ssh\optimizesolux_vps_ed25519` |
| Stack | `/opt/notification-hub/prod` |
| Scripts | `/opt/notification-hub/deploy` |
| Compose | `docker-compose.prod.yml` / project `notification-hub-prod` |

URLs : app `https://notification.optimizesolux.com` · API `https://notification-api.optimizesolux.com` · auth `https://notification-auth.optimizesolux.com`

## Deploy routine (images déjà en GHCR)

```bash
bash /opt/notification-hub/deploy/deploy.sh prod \
  ghcr.io/<org>/notification-hub-frontend:<FULL_SHA> \
  ghcr.io/<org>/notification-hub-backend:<FULL_SHA>
```

Éviter `init.sh` pour un simple bump d’image (réécrit / injecte des secrets).

## Anti-patterns

- Logger `/opt/notification-hub/prod/.env`
- Forcer `promote` CD sans images CI publiées
