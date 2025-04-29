# Vaultiq-Session

`vaultiq-session` is a robust session management library designed for applications needing device-based session handling. It supports both **JPA** and **Redis** persistence backends, allowing seamless integration into your Spring Boot applications. The library offers flexibility in how sessions are managed and stored, supporting cache management and entity persistence for device sessions.

## Features

🔐 **Session Management**: Creates and manages device sessions for users, with support for updating activity and deletion.  
🚀 **Redis Support**: Use Redis for in-memory session caching, optimizing performance.  
🗄️ **JPA Support**: Persist sessions in a relational database via JPA for reliable storage.  
🧠 **Caching Layer (JPA)**: Optional in-memory cache for JPA sessions using a named `CacheManager`.  
🖥️ **Device Fingerprinting**: Ensures that sessions are tied to specific devices using customizable device fingerprints.  
🔗 **Seamless Integration**: Integrates effortlessly with Vaultiq’s ecosystem and any Spring Boot application.

## Table of Contents

- [Installation](#installation)
- [Configuration](#configuration)
  - [Redis Support](#redis-support)
  - [JPA Support](#jpa-support)
- [Usage](#usage)
  - [Session Management](#session-management)
  - [Device Fingerprinting](#device-fingerprinting)
- [Contributing](#contributing)
- [License](#license)

## Installation

Coming soon, via Maven Central.

## Configuration

### Redis Support

To use Redis for session management, you need to configure a `CacheManager` bean in your application.

#### Steps:
1. Add the required Redis dependency to your project:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-data-redis</artifactId>
   </dependency>
   <dependency> <!-- not yet available -->
      <groupId>com.vaultiq</groupId>
      <artifactId>vaultiq-session</artifactId>
      <version>1.0.0</version>
   </dependency>
   ```

2. Configure your `CacheManager` bean:
   ```java
   @Bean
   public CacheManager vaultiqCacheManager(RedisConnectionFactory redisConnectionFactory) {
       // configure and return CacheManager for Redis
   }
   ```

3. Enable Redis session management in your configuration:
   ```yaml
   vaultiq:
     session:
       persistence:
         via-redis:
           enabled: true
           allow-inflight-cache-management: true
           cache-manager-name: vaultiqCacheManager
           cache-name: vaultiq-session-pool
   ```

### JPA Support

To use JPA for session management, add the following dependencies:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency> <!-- not yet available -->
    <groupId>com.vaultiq</groupId>
    <artifactId>vaultiq-session</artifactId>
    <version>1.0.0</version>
</dependency>
```

Enable JPA and optional caching support in your configuration:

```yaml
vaultiq:
  session:
    persistence:
      via-jpa:
        enabled: true
        allow-inflight-entity-creation: true
        enable-caching: true
        cache-manager-name: vaultiqCacheManager
        cache-name: vaultiq-session-pool
```

This allows the library to persist session data in a relational database and cache sessions using the configured `CacheManager`.

## Usage

### Session Management

Interact with the `VaultiqSessionManager` interface, which provides the following methods:

- `createSession(String userId, HttpServletRequest request)` - Creates a new session for a user.
- `getSession(String sessionId)` - Retrieves a session by ID.
- `deleteSession(String sessionId)` - Deletes a session.
- `updateToCurrentlyActive(String sessionId)` - Updates the last active timestamp.
- `getSessionsByUser(String userId)` - Gets all sessions for a user.

```java
@Autowired
private VaultiqSessionManager sessionManager;

VaultiqSession session = sessionManager.createSession("user123", request);
VaultiqSession existingSession = sessionManager.getSession(sessionId);
sessionManager.updateToCurrentlyActive(sessionId);
sessionManager.deleteSession(sessionId);
List<VaultiqSession> userSessions = sessionManager.getSessionsByUser("user123");
```

### Device Fingerprinting

The library uses device fingerprints to tie sessions to specific devices.

- Implement `DeviceFingerprintGenerator` to customize fingerprint logic.
- Implement `DeviceFingerprintValidator` to validate fingerprints if needed.

```java
@Component
public class CustomDeviceFingerprintGenerator implements DeviceFingerprintGenerator {
    @Override
    public String generateFingerprint(HttpServletRequest request) {
        return request.getHeader("User-Agent") + "::" + request.getRemoteAddr();
    }
}
```

## Contributing

We welcome contributions. To get started:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes.
4. Commit (`git commit -am 'Add new feature'`).
5. Push (`git push origin feature/your-feature`).
6. Open a pull request.

## License

This library is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.

