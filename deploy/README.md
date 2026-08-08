# Deploy Contabo — Notification Hub

Default path: **Docker Compose + shared-traefik** (pas K8s).

## Layout serveur

```
/opt/notification-hub/
  init.sh
  deploy/          # sync depuis GitHub
  prod/
    .env
    releases/
```

## Scripts

| Script | Rôle |
|--------|------|
| `init.sh` | Bootstrap CD : sync + setup 1re fois + `deploy.sh` |
| `setup-server.sh` | Docker networks, `.env`, détection shared-traefik |
| `deploy.sh` | Pull images GHCR + `compose up` + smoke HTTP |
| `update-deploy.sh` | Clone `deploy/` depuis GitHub (swap atomique) |

## Manual promote (SSH)

Préférer le CD GitHub ; en secours :

```bash
sudo /opt/notification-hub/init.sh prod \
  ghcr.io/<org>/notification-hub-frontend:<sha> \
  ghcr.io/<org>/notification-hub-backend:<sha> \
  --ghcr-username ... --ghcr-token ...
```

Voir [GITHUB-SECRETS-CONTABO.md](./GITHUB-SECRETS-CONTABO.md).

## K8s

Manifests optionnels : [k8s/](./k8s/).
