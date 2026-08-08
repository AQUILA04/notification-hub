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

Le bean `NotificationHubClient` expose aussi `get`, `list` et `events`.
