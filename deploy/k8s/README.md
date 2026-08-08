# Optional K8s activation

Manifests live in this folder. Default Contabo production path is **Docker Compose**.

## Apply (when needed)

```bash
kubectl apply -f deploy/k8s/notification-hub.yaml
```

Prerequisites (not included): PostgreSQL, Redis, Artemis, Keycloak reachable from the cluster
(operators, Helm charts, or external Contabo VMs). Update `ConfigMap` / `Secret` and the image
reference before applying.

## Hardening included (P3)

| Resource | Role |
|----------|------|
| `PodDisruptionBudget` | `minAvailable: 1` pendant les drains |
| `NetworkPolicy` | Ingress API :8088 ; egress DNS + deps (PG/Redis/Artemis/SMTP/OIDC) |
| `securityContext` | non-root, drop ALL capabilities |
| ConfigMap | quotas, circuit breaker, coûts estimés, `WHATSAPP_ENABLED` |

Ajuste les sélecteurs egress si Postgres/Redis sont hors namespace.

## Switch back to Compose

Use the root `docker-compose.yml` — no cluster required.
