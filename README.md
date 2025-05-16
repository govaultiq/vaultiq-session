# Vaultiq-Session

**`vaultiq-session`** is a lightweight, plug-and-play session management library for Spring Boot applications that supports device-aware tracking with flexible persistence options like **JPA**, **caching**, or both.

### [What's the difference between `sid` and `jti`? Which is better?](https://github.com/govaultiq/vaultiq-session/blob/main/sid_vs_jti.md)

<br>

## 🚀 Features

* ✅ **Device-Aware Session Tracking** — Isolate sessions by browser/device
* 📱 **Extended Device Metadata** — Captures device model and operating system
* 🗄️ **JPA Support** — Persistent session storage for audit/compliance needs
* ⚡ **Cache Support (Redis, etc.)** — Fast in-memory session lookups
* 🔀 **Hybrid Mode** — Durability + Speed
* 🧠 **Smart Fingerprinting** — Defaults built-in, easily overrideable
* 🛡️ **Secure** — Built-in support for session blocklists with per-model overrides
* 🪄 **Zen Mode** — One-line configuration with override capabilities
* 🧩 **Fully Extensible** — Plug in your logic where needed
* 🧰 **Device Metadata Available in Sessions** — Access OS & device model from `VaultiqSession`

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

### ▶ Zen Mode (Minimal Config + Powerful Defaults)

```yaml
vaultiq:
  session:
    zen-mode: true
```

> Defaults:
>
> * `use-jpa` and `use-cache` = true
> * `cache-manager` = "cacheManager"
> * Default cache names per model

### ▶ Model-Level Override (for blocklist, mapping, etc.)

```yaml
vaultiq:
  session:
    zen-mode: true
    persistence:
      models:
        - type: BLOCKLIST
          use-jpa: false
```

### ▶ Full Manual Mode (Complete Control)

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
          cache-name: "session-pool"
          use-cache: false
          use-jpa: true

        - type: USER_SESSION_MAPPING
          cache-name: "user-session-mapping"
          use-jpa: false

        - type: BLOCKLIST
          use-jpa: false
```

<br>

## 🔐 User Identity

> Required: Implement `UserIdentityAware` to enable session tracking per user context.

```java
@Component
public class SecurityContextUserIdentity implements UserIdentityAware {
    @Override
    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

<br>

## 🧠 Custom Fingerprinting

Override device fingerprinting logic:
**Note:** Both the Generator and Validator must be implemented for successful custom fingerprint validation.

```java
@Component
public class MyDeviceFingerprintGenerator implements DeviceFingerprintGenerator {
    @Override
    public String generateFingerprint(HttpServletRequest request) {
        return request.getRemoteAddr() + "::" + request.getHeader("User-Agent");
    }
}
```

<br>

## 📍 Usage

```java
@Autowired
private VaultiqSessionManager sessionManager;

VaultiqSession session = sessionManager.createSession("user123", request);
VaultiqSession fetched = sessionManager.getSession(session.getSessionId());
sessionManager.deleteSession(session.getSessionId());
List<VaultiqSession> sessions = sessionManager.getSessionsByUser("user123");
int count = sessionManager.totalUserSessions("user123");
```

### 📎 Login Integration Example

```java
@PostMapping("/login")
public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    VaultiqSession session = sessionManager.createSession(user.getId(), httpRequest);
    return new LoginResponse(generateJwt(user, session.getSessionId()));
}
```

<br>

## 📈 Performance Tips

| Mode      | Use Case                                 |
| --------- | ---------------------------------------- |
| JPA       | Durability-first, low traffic            |
| Cache     | High throughput, short session lifespans |
| Hybrid 🔥 | Best of both for most production apps    |

<br>

## 🤝 Contributing

1. Fork the repo
2. Create a feature branch: `git checkout -b feature/amazing`
3. Commit and push
4. Open a pull request 🚀

<br>

## 📝 License

Licensed under the [MIT License](LICENSE)
