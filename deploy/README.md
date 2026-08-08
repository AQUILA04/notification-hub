# Deploy Contabo — Notification Hub

Default path: **Docker Compose + shared-traefik + optimize-common-infra** (pas K8s).

## Layout serveur

```
/opt/notification-hub/
  init.sh
  deploy/          # sync depuis GitHub
  prod/
    .env
    releases/

/opt/optimizesolux/common-infra/   # Redis, Artemis, Keycloak, …
```

## Scripts

| Script | Rôle |
|--------|------|
| `init.sh` | Bootstrap CD : sync + setup 1re fois + `deploy.sh` |
| `setup-server.sh` | Networks `traefik-public` + `optimizesolux-common`, `.env` |
| `deploy.sh` | Pull images GHCR + `compose up` + smoke HTTP |
| `update-deploy.sh` | Clone `deploy/` depuis GitHub (swap atomique) |

## Prod vs local

| | Contabo (`deploy/docker-compose.prod.yml`) | Laptop (`docker-compose.yml`) |
|--|---------------------------------------------|-------------------------------|
| Conteneurs | API + FE + Postgres métier | Postgres + Redis + Artemis + Mailpit + Keycloak (+ API profile) |
| Auth | `auth.optimizesolux.com` | `localhost:8081` |
| Réseaux | `optimizesolux-common` + `traefik-public` | bridge local |

## Manual promote (SSH)

Préférer le CD GitHub ; en secours :

```bash
sudo /opt/notification-hub/init.sh prod \
  ghcr.io/<org>/notification-hub-frontend:<sha> \
  ghcr.io/<org>/notification-hub-backend:<sha> \
  --ghcr-username ... --ghcr-token ... \
  --redis-password ... --artemis-password ...
```

Voir [GITHUB-SECRETS-CONTABO.md](./GITHUB-SECRETS-CONTABO.md).

## K8s

Manifests optionnels : [k8s/](./k8s/).
