# Vaultiq-Session

`vaultiq-session` is a robust session management library designed for applications needing device-based session handling. It supports both **Redis** and **JPA** persistence backends, allowing seamless integration into your Spring Boot applications. The library offers flexibility in how sessions are managed and stored, supporting cache management and entity persistence for device sessions.

## Features

- **Session Management**: Creates and manages device sessions for users, with support for updating activity and deletion.
- **Redis Support**: Use Redis for in-memory session caching, optimizing performance.
- **JPA Support**: Persist sessions in a relational database via JPA for reliable storage.
- **Device Fingerprinting**: Ensures that sessions are tied to specific devices using customizable device fingerprints.
- **Seamless Integration**: Integrates effortlessly with Vaultiq’s ecosystem and any Spring Boot application.

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

coming soon, via Maven Central.
<!-- To include `vaultiq-session` in your project, add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.vaultiq</groupId>
    <artifactId>vaultiq-session</artifactId>
    <version>1.0.0</version>
</dependency>
```


Or, if you're using Gradle:

```groovy
implementation 'com.vaultiq:vaultiq-session:1.0.0'
```
-->

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
   ```

2. Configure your `CacheManager` bean in your application:
   ```java
   @Bean
   public CacheManager vaultiqCacheManager(RedisConnectionFactory redisConnectionFactory) {
       return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                               .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
                               .build();
   }
   ```

3. Enable Redis session management in `application.yml` or `application.properties`:
   ```yaml
   vaultiq:
     session:
       persistence:
         via-redis:
           allow-inflight-cache-management: true
   ```

This will configure the session management via Redis and use the existing `CacheManager` bean when available.

### JPA Support

To use JPA for session management, add the following dependencies to your project:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>com.vaultiq</groupId>
    <artifactId>vaultiq-session</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then, configure the JPA support in your application properties:

```yaml
vaultiq:
  session:
    persistence:
      via-jpa:
        allow-inflight-entity-creation: true
```

This will allow the library to persist session data in a relational database using JPA.

## Usage

### Session Management

To manage user sessions, you can interact with the `VaultiqSessionManager` interface, which provides the following methods:

- `createSession(String userId, HttpServletRequest request)` - Creates a new session for a user.
- `getSession(String sessionId)` - Retrieves an existing session by its ID.
- `updateSession(VaultiqSession session)` - Updates an existing session (e.g., updating the last active time).
- `deleteSession(String sessionId)` - Deletes a session by its ID.
- `getSessionsByUser(String userId)` - Retrieves all sessions associated with a user.

```java
@Autowired
private VaultiqSessionManager sessionManager;

// Creating a session
VaultiqSession session = sessionManager.createSession("user123", request);

// Retrieving a session
VaultiqSession existingSession = sessionManager.getSession(sessionId);

// Updating a session
sessionManager.updateSession(existingSession);

// Deleting a session
sessionManager.deleteSession(sessionId);

// Get sessions by user
List<VaultiqSession> userSessions = sessionManager.getSessionsByUser("user123");
```

### Device Fingerprinting

The library uses device fingerprints to ensure that sessions are tied to specific devices. You can configure the fingerprint generator and validator to meet your requirements.

1. **DeviceFingerprintGenerator**: This interface is used to generate the fingerprint of a device based on request headers.
   
   You can implement your own fingerprint generator or use the default provided by the library.

2. **DeviceFingerprintValidator**: This interface is used to validate whether a session corresponds to a specific device by comparing the stored fingerprint with the request's fingerprint.

### Example of Custom Device Fingerprint Implementation

You can customize the fingerprint generation as follows:

```java
@Component
public class CustomDeviceFingerprintGenerator implements DeviceFingerprintGenerator {

    @Override
    public String generateFingerprint(HttpServletRequest request) {
        // Implement your own fingerprint logic here
        return "custom-fingerprint";
    }
}
```

## Contributing

We welcome contributions to improve the `vaultiq-session` library. If you would like to contribute, please fork the repository and submit a pull request. 

Here’s how to get started:

1. Fork the repository.
2. Create a new branch (`git checkout -b feature/your-feature`).
3. Make your changes.
4. Commit your changes (`git commit -am 'Add new feature'`).
5. Push to the branch (`git push origin feature/your-feature`).
6. Open a pull request.

## License

This library is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
