# Vaultiq-Session

**`vaultiq-session`** is a flexible, pluggable session management library for Spring Boot applications that simplifies per-device session tracking with support for **JPA**, **in-memory caching**, or a hybrid approach.

<br>

## 🔧 Features

- ✅ **Device-Based Session Management** - Track and manage sessions per device
- 📔 **JPA Support** - Durable, persistent storage for session data
- ⚡ **Cache Support** - High-speed lookups with Redis or other cache providers
- 🔁 **Hybrid JPA + Cache Mode** - Get the best of both worlds for performance and durability
- 🖥️ **Smart Device Fingerprinting** - Built-in logic with customization options
- 🔒 **Session Security** - Efficient blocklisting mechanisms for terminated sessions
- 🔗 **Seamless Integration** - Works with Vaultiq's ecosystem and any Spring Boot application
- ✨ **Fully Configurable** - Managed internally but easily customizable

<br>

## 📦 Installation

Coming soon on Maven Central.

```xml
<dependency>
  <groupId>com.vaultiq</groupId>
  <artifactId>vaultiq-session</artifactId>
  <version>1.0.0</version>
</dependency>
```

<br>

## ⚙️ Configuration

Use the following structure in your `application.yml` to enable your preferred session management mode:

### Option 1: JPA-Only Mode

```yaml
vaultiq:
  session:
    persistence:
      jpa:
        enabled: true
      cache:
        enabled: false
```

### Option 2: Cache-Only Mode (e.g., Redis)

```yaml
vaultiq:
  session:
    persistence:
      jpa:
        enabled: false
      cache:
        enabled: true
        manager: vaultiqCacheManager
        sessionPool: vaultiq-session-pool
        blocklistPool: vaultiq-blocklist-pool
```

> ⚠️ When using cache-only mode, you **must implement** `UserIdentityAware` to provide current user context.

```java
@Component
public class SecurityContextUserIdentity implements UserIdentityAware {
    @Override
    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

### Option 3: Hybrid Mode (JPA + Cache)

```yaml
vaultiq:
  session:
    persistence:
      jpa:
        enabled: true
      cache:
        enabled: true
        manager: vaultiqCacheManager
        sessionPool: vaultiq-session-pool
        blocklistPool: vaultiq-blocklist-pool
```

<br>

## 🧠 Custom Device Fingerprinting

The default fingerprinting works out of the box, but you can implement your own strategy:

```java
@Component
public class MyDeviceFingerprintGenerator implements DeviceFingerprintGenerator {
    @Override
    public String generateFingerprint(HttpServletRequest request) {
        // Your custom implementation here
        // E.g., combine IP, user-agent, and other device identifiers
        return customFingerprint;
    }
}
```

<br>

## 🚀 Usage Examples

### Basic Operations

Inject the session manager in your components:

```java
@Autowired
private VaultiqSessionManager sessionManager;
```

Create and manage sessions:

```java
// Create a new session for a user
VaultiqSession session = sessionManager.createSession("user123", request);

// Retrieve an existing session
VaultiqSession fetched = sessionManager.getSession(session.getSessionId());

// Mark a session as currently active
sessionManager.updateToCurrentlyActive(fetched.getSessionId());

// End a session
sessionManager.deleteSession(fetched.getSessionId());

// List all sessions for a user
List<VaultiqSession> userSessions = sessionManager.getSessionsByUser("user123");

// Count active sessions
int sessionCount = sessionManager.totalUserSessions("user123");
```

### Integration with Authentication

```java
@RestController
public class AuthController {
    @Autowired
    private VaultiqSessionManager sessionManager;
    
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        // Authenticate user...
        
        // Create a new session
        VaultiqSession session = sessionManager.createSession(user.getId(), httpRequest);
        
        return LoginResponse.builder()
            .token(generateJwt(user, session.getSessionId()))
            .build();
    }
}
```

<br>

## 📊 Performance Considerations

- **JPA-Only Mode**: Best for applications where durability is critical
- **Cache-Only Mode**: Optimal for high-traffic applications requiring speed
- **Hybrid Mode**: Recommended for most production deployments to balance performance and reliability

<br>

## 🤝 Contributing

1. Fork and clone the repo
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a pull request

<br>

## 📝 License

Licensed under the [MIT License](LICENSE).
