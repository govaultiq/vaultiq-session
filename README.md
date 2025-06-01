# Vaultiq-Session

**`vaultiq-session`** is a lightweight, plug-and-play session management library for Spring Boot applications that supports device-aware tracking with flexible persistence options such as **JPA**, **caching**, or both.

### [What's the difference between ](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)[`sid`](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)[ and ](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)[`jti`](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)[? Which is better?](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)

<br>

## ✨ Features

* **Device-aware session tracking** — isolate sessions by browser/device
* **Extended device metadata** — capture device model, OS, and more
* **JPA support** — durable session persistence
* **Cache support** — fast lookups via Redis or other Spring-compatible caches
* **Hybrid mode** — combine persistence and speed
* **Smart fingerprinting** — built-in defaults with override capability
* **Secure revocation** — fine-grained session invalidation
* **Production mode** — minimal configuration, maximum effect

<br>

## 📦 Installation

Soon to be available on Maven Central:

```xml
<dependency>
  <groupId>com.vaultiq</groupId>
  <artifactId>vaultiq-session</artifactId>
  <version>1.0.0</version>
</dependency>
```

<br>

## ⚙️ Configuration

### Production Mode (minimal config + powerful defaults—Production Ready)

```yaml
vaultiq:
  session:
    production-mode: true
```

> Production mode enables both JPA and cache with sensible defaults.
>
> * `cache-manager` = "cacheManager"
> * Cache names are derived from the `CacheType.alias()` method (see CacheType here: [CacheType](https://github.com/govaultiq/vaultiq-session/blob/main/src/main/java/vaultiq/session/cache/util/CacheType.java))
> * Ensure caches are **registered in the Spring CacheManager** with matching names

### Model-Level Overrides

```yaml
vaultiq:
  session:
    production-mode: true
    persistence:
      models:
        - type: REVOKE
          use-jpa: false
```
> Here, `type` takes a ModelType name as its value. See ModelType here: [ModelType](https://github.com/govaultiq/vaultiq-session/blob/main/src/main/java/vaultiq/session/model/ModelType.java)

### Full Manual Mode

```yaml
vaultiq:
  session:
    persistence:
      cache-config:
        manager: "vaultiqCacheManager"
        use-jpa: true
        use-cache: true
      models:
        - type: SESSION
          use-cache: false
          use-jpa: true
        - type: REVOKE
          use-jpa: false
```

<br>

## 🔐 User Identity

Implement `UserIdentityAware` to enable user-context-based session tracking.

```java
@Component
public class SecurityContextUserIdentity implements UserIdentityAware {
    @Override
    public String getCurrentUserId() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
            .map(Authentication::getName)
            .orElse(null);
    }
}
```

<br>

## 📍 Usage

```java
@Autowired
private SessionApi sessionApi;

// Creates a new session for the user and device
ClientSession session = sessionApi.createSession("user123", httpRequest);

// Validates the current session from request headers/cookies
Boolean isValid = sessionApi.validate(httpRequest);

// Revokes a specific session by ID
sessionApi.revoke(RevocationRequest.revoke("sessionId").withNote("User logged out"));

// Revokes all sessions for a specific user
sessionApi.revoke(RevocationRequest.revokeAll("userId").withNote("Full logout"));

// Revokes all sessions except the provided ones
sessionApi.revoke(RevocationRequest.revokeAllExcept("sessionId1", "sessionId2").withNote("Keep some active"));
```

### Login Integration Example

```java
@PostMapping("/login")
public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    VaultiqSession session = sessionManager.createSession(user.getId(), httpRequest);
    return new LoginResponse(generateJwt(user, session.getSessionId()));
}
```

<br>

## 🔄 Optional Cleanup

You can configure automatic cleanup of expired or revoked sessions using the clean-up block in YAML. If omitted, the library won’t register any background jobs.

Retention controls how long sessions are kept after being revoked. Jobs run only if configured.

## 📊 Performance Tips

| Mode      | Use Case                              |
| --------- | ------------------------------------- |
| JPA       | Durable, low-traffic apps             |
| Cache     | High throughput, short-lived sessions |
| Hybrid 🔥 | Best of both for real-world needs     |

<br>

## 🤝 Contributing

1. Fork the repo.
2. Create your feature branch.
3. Push your changes.
4. Open a pull request.

<br>

## 📍 License

Licensed under the [MIT License](LICENSE)
