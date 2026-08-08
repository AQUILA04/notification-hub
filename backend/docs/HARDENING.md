# Quotas, circuit breakers & coûts (P3)

## Quotas (Redis)

Fenêtre glissante **1 minute**, clé :

`nhub:rl:{tenant}:{CHANNEL}:{app}:{yyyyMMddHHmm}`

App id :

1. Header `X-App-Id`
2. sinon `metadata.appId` / `metadata.app`
3. sinon `default`

Dépassement → **429** `QUOTA_EXCEEDED`.

```yaml
notification-hub:
  quota:
    enabled: true
    default-per-minute: 120
    per-channel-per-minute:
      email: 200
      sms: 60
      whatsapp: 30
```

Env : `QUOTA_ENABLED`, `QUOTA_*_PER_MINUTE`.

## Circuit breakers (Redis)

Clé logique `{channel}:{provider}` (ex. `sms:afriksms`).

- CLOSED → compte les échecs provider
- seuil `failure-threshold` → OPEN pendant `open-duration-seconds`
- puis HALF_OPEN (1 probe) → succès = CLOSED, échec = OPEN

État : `GET /v1/admin/circuits`

Env : `CIRCUIT_BREAKER_ENABLED`, `CIRCUIT_BREAKER_FAILURE_THRESHOLD`, `CIRCUIT_BREAKER_OPEN_SECONDS`.

## Coût estimé KPI

`GET /v1/admin/kpi` ajoute `estimatedCostByChannel` et `estimatedCostTotal`
(config `notification-hub.cost.*` / `COST_EMAIL|SMS|WHATSAPP`).

## Audit

Table `audit_events` (Flyway V3). Actions : `NOTIFICATION_CREATE`, `NOTIFICATION_REPLAY`,
`DLQ_REQUEUE`, `DLQ_DISCARD`. Actor = JWT `preferred_username` / `sub` ou `anonymous`.
