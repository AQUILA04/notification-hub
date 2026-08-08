# Notification Hub — console ops (Angular 21)

## Dev

```bash
# API must be running on :8088 (profile local)
npm start
```

Open http://localhost:4200 — choose the tenant in the sidebar (`demo-tenant` by default).
Calls send header `X-Tenant-Id` (no Keycloak yet; planned with Spring profile `docker`).

## Screens

- **Dashboard** — `GET /v1/admin/kpi`
- **Notifications** — list + filters + detail Event Store
- **Templates** — create / list / preview Pebble

## Build

```bash
npm run build
```
