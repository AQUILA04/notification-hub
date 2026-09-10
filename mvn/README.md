# Maven clients — Notification Hub

Clients Spring Boot pour intégrer **notification-hub** depuis d’autres applications Optimize.
Les stacks `npm/` et `gradle/` pourront coexister au même niveau du monorepo.

## Modules

| Artifact | Rôle |
|----------|------|
| `sb-notification-hub-starter` | Dépendance à ajouter dans l’app consommatrice |
| `sb-notification-hub-autoconfigure` | Auto-config, modèles, client HTTP (transitivement inclus) |

- **groupId** : `com.optimize.notification`
- **version** : `0.1.0-SNAPSHOT`
- **Java** : 21+ · **Spring Boot** : 3.5.x

## Build

```bash
cd mvn
mvn clean install
```

CI (GitHub Actions) — [`.github/workflows/mvn-clients.yml`](../.github/workflows/mvn-clients.yml) :

| Déclencheur | Action |
|-------------|--------|
| Push / PR sur `mvn/**` | `mvn clean install` + JARs en artifacts du run |
| **Run workflow** (manuel) | `mvn clean deploy` → **GitHub Packages** |

Consommation depuis une autre app (après deploy manuel) :

```xml
<repository>
  <id>github</id>
  <url>https://maven.pkg.github.com/OWNER/notification-hub</url>
</repository>
```

## Usage dans une app Spring Boot

```xml
<dependency>
  <groupId>com.optimize.notification</groupId>
  <artifactId>sb-notification-hub-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Configuration (prod — service account Keycloak)

```yaml
optimize:
  notification:
    hub:
      base-url: https://notification-hub.example.com
      oauth2:
        token-uri: https://auth.example.com/realms/notification-hub/protocol/openid-connect/token
        client-id: my-app
        client-secret: ${NOTIFICATION_HUB_CLIENT_SECRET}
```

Équivalents env (relaxed binding) :

```bash
OPTIMIZE_NOTIFICATION_HUB_BASE_URL=...
OPTIMIZE_NOTIFICATION_HUB_OAUTH2_TOKEN_URI=...
OPTIMIZE_NOTIFICATION_HUB_OAUTH2_CLIENT_ID=...
OPTIMIZE_NOTIFICATION_HUB_OAUTH2_CLIENT_SECRET=...
```

### Configuration (local — sans OAuth2)

```yaml
optimize:
  notification:
    hub:
      base-url: http://localhost:8088
      tenant-id: demo-tenant
      oauth2:
        enabled: false
```

### Appel

```java
@Service
public class OrderNotifier {
  private final NotificationHubClient hub;

  public OrderNotifier(NotificationHubClient hub) {
    this.hub = hub;
  }

  public void notifyPaid(String email) {
    hub.send(
        CreateNotificationRequest.builder()
            .channel(Channel.EMAIL)
            .from("noreply@example.com")
            .to(email)
            .subject("Payment received")
            .body("<p>Thanks</p>")
            .build(),
        UUID.randomUUID().toString());
  }
}
```

Le bean `NotificationHubClient` expose aussi `get`, `list`, `events`, **`sendOtp`** et **`verifyOtp`**.

### Environnement SMS (`test` / `prod`)

Pour le canal **SMS** (notifications et OTP), le hub exige un `environment` :

| Valeur | Effet |
|--------|--------|
| omis / `test` / autre chose que `prod` | **Pas de SMS réel** — message intercepté vers **Mailpit** (SMTP du hub) |
| `prod` | SMS réel (Brevo / provider configuré) |

Le starter **injecte automatiquement** cette valeur d’après le profil Spring Boot actif :

- profil `prod` ou `production` → `environment=prod`
- tout autre profil (`local`, `dev`, `test`, défaut, …) → `environment=test`

Surcharge optionnelle :

```yaml
optimize:
  notification:
    hub:
      environment: test   # force test même avec spring.profiles.active=prod
```

Un `environment` déjà posé sur `CreateNotificationRequest` / `OtpSendRequest` n’est **pas** écrasé.

```java
hub.send(
    CreateNotificationRequest.builder()
        .channel(Channel.SMS)
        .from("OptimizeSLX")
        .to("+22890909090")
        .body("OTP 4242")
        .build(),              // environment injecté = test hors profil prod
    UUID.randomUUID().toString());
```

### OTP (envoi + vérification)

Le hub génère et envoie le code ; votre app ne manipule jamais le code en clair côté serveur
(sauf pour le transmettre à `/verify` après saisie utilisateur).

```java
@Service
public class OtpLoginService {
  private final NotificationHubClient hub;

  public OtpLoginService(NotificationHubClient hub) {
    this.hub = hub;
  }

  public void sendCode(String phoneE164) {
    hub.sendOtp(OtpSendRequest.sms(phoneE164), UUID.randomUUID().toString());
  }

  public boolean checkCode(String phoneE164, String code) {
    return hub.verifyOtp(OtpVerifyRequest.of(phoneE164, code)).valid();
  }
}
```

| Méthode client | Endpoint hub | HTTP |
|----------------|--------------|------|
| `sendOtp(request)` / `sendOtp(request, idempotencyKey)` | `POST /v1/otp/send` | 202 |
| `verifyOtp(request)` | `POST /v1/otp/verify` | 200 |

Guide complet (auth, schémas JSON, erreurs, curl, checklist) :
[backend/docs/OTP_CLIENT_INTEGRATION.md](../backend/docs/OTP_CLIENT_INTEGRATION.md)
