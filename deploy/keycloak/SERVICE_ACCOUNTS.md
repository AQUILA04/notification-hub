# Service accounts — applications appelantes

Chaque plateforme qui envoie des notifications (CleanTrack, BCMS, Elykia, …)
s’authentifie auprès de Keycloak avec un **client confidentiel + Service Account**
(OAuth2 Client Credentials), puis appelle l’API avec le Bearer token.

Le hub lit le claim **`tenant_id`** du JWT (sinon le header `X-Tenant-Id` en
profil `local` uniquement).

## Prérequis

- Keycloak 26.* — realm `notification-hub`
  - **Local** : import `realm-notification-hub.json` via `docker compose` (port 8081)
  - **Prod Contabo** : realm fourni par `optimize-common-infra` (`auth.optimizesolux.com`)
- Console admin local : http://localhost:8081 — user `admin` / `admin`
- Rôles realm déjà créés : `notification-sender`, `notification-admin`

## Recommandation

| Approche | Quand |
|----------|--------|
| **1 client confidential par app** (`clean-track-pro`, `bcms`, …) | Production — isolation secrets, `tenant_id` et audit par app |
| Client partagé `notification-hub-apps` | Démo / smoke test uniquement |

Ne jamais committer le client secret. Le stocker dans Vault / secrets Compose / K8s.

---

## Créer un service account (UI Keycloak)

1. **Clients → Create client**
   - Client type : OpenID Connect  
   - Client ID : ex. `clean-track-pro`  
   - Name : `CleanTrack Pro (notification sender)`

2. **Capability config**
   - Client authentication : **On** (confidential)  
   - Authentication flow : **désactiver** Standard flow / Direct access  
   - **Service accounts roles** : **On**  
   - Authorization : Off (sauf besoin fine-grained)

3. **Login settings**
   - Root / Home / Redirect : laisser vides (pas de browser login)

4. **Credentials**
   - Onglet Credentials → copier le **Client secret**

5. **Service account roles**
   - Onglet **Service account roles**  
   - Assign role → Filter by realm roles → `notification-sender`  
   - (Ops / templates) ajouter aussi `notification-admin` si l’app gère les templates

6. **Claim `tenant_id`**
   - Onglet **Client scopes** → Dedicated scope du client → **Add mapper** →  
     **By configuration → Hardcoded claim**  
   - Claim name : `tenant_id`  
   - Claim value : ex. `cleantrack` (stable, kebab/snake OK)  
   - Claim JSON type : String  
   - Add to access token : **On**  
   - Add to ID token / userinfo : optionnel

   Alternative : mapper **User Attribute** sur l’utilisateur technique
   `service-account-<clientId>` si le tenant doit être dynamique.

7. **Audience (recommandé en prod)**
   - Mapper **Audience** → Included Client Audience : `notification-hub-api`  
   - Vérifier côté API que le JWT est accepté (issuer = realm).

---

## Créer un service account (Admin REST API)

Remplacer `KEYCLOAK`, `REALM`, `ADMIN_TOKEN`, `CLIENT_ID`, `TENANT`.

```bash
# 1) Token admin
ADMIN_TOKEN=$(curl -s -X POST "$KEYCLOAK/realms/master/protocol/openid-connect/token" \
  -d "grant_type=password" \
  -d "client_id=admin-cli" \
  -d "username=admin" \
  -d "password=admin" | jq -r .access_token)

# 2) Créer le client confidential + service account
curl -s -X POST "$KEYCLOAK/admin/realms/$REALM/clients" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"clientId\": \"$CLIENT_ID\",
    \"name\": \"$CLIENT_ID notification sender\",
    \"enabled\": true,
    \"publicClient\": false,
    \"secret\": \"$CLIENT_SECRET\",
    \"serviceAccountsEnabled\": true,
    \"standardFlowEnabled\": false,
    \"directAccessGrantsEnabled\": false,
    \"protocol\": \"openid-connect\"
  }"
```

Puis dans la console (ou via API) : assigner `notification-sender` au service
account user, et ajouter le mapper `tenant_id` (hardcoded claim).

Récupérer le secret généré :

```bash
# UUID interne du client
CID=$(curl -s "$KEYCLOAK/admin/realms/$REALM/clients?clientId=$CLIENT_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.[0].id')

curl -s "$KEYCLOAK/admin/realms/$REALM/clients/$CID/client-secret" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r .value
```

---

## Obtenir un token et appeler le Hub

```bash
TOKEN=$(curl -s -X POST \
  "$KEYCLOAK/realms/notification-hub/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials" \
  -d "client_id=clean-track-pro" \
  -d "client_secret=$CLIENT_SECRET" | jq -r .access_token)

# Décoder pour vérifier tenant_id (jwt.io / jq + base64)
curl -s -X POST "$HUB/v1/notifications" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
    "channel": "EMAIL",
    "from": "noreply@cleantrack.pro",
    "to": ["client@exemple.com"],
    "subject": "Test",
    "body": "<p>Hello</p>"
  }'
```

Avec le profil Spring **`docker`** (JWT obligatoire) : ne pas compter sur
`X-Tenant-Id` — le tenant vient du token.

Avec le profil **`local`** (dev) : JWT optionnel ; `X-Tenant-Id` suffit.

---

## Client seed du realm importé

| Client ID | Usage |
|-----------|--------|
| `notification-hub-api` | Resource server (bearer-only) |
| `notification-hub-apps` | Démo service account — secret `change-me-service-secret`, `tenant_id=demo-tenant` |
| `notification-hub-console` | Angular public (P1) |

**Changer le secret** de `notification-hub-apps` dès qu’un environnement
n’est plus purement local.

Exemple token démo :

```bash
curl -s -X POST \
  "http://localhost:8081/realms/notification-hub/protocol/openid-connect/token" \
  -d "grant_type=client_credentials" \
  -d "client_id=notification-hub-apps" \
  -d "client_secret=change-me-service-secret"
```

---

## Checklist intégration d’une nouvelle app

- [ ] Client confidential créé (1 par app)
- [ ] Service accounts enabled
- [ ] Rôle `notification-sender` (et `notification-admin` si besoin)
- [ ] Mapper `tenant_id` dans l’access token
- [ ] Secret stocké hors git
- [ ] App utilise Client Credentials + `Authorization: Bearer`
- [ ] App envoie `Idempotency-Key` sur chaque POST notification
- [ ] `from` validé / autorisé côté métier (le hub exige le champ)
