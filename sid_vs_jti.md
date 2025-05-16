# Session Awareness in JWT: `sid` vs `jti` — A Practical Comparison

## Introduction

In modern authentication systems using JWT (JSON Web Tokens), managing token revocation effectively is crucial for security and user experience. Two common identifiers used for token awareness and revocation are:

* **`jti` (JWT ID):** A unique identifier for each individual token.
* **`sid` (Session ID):** An identifier representing a user session or device context, shared by all tokens issued for that session.

This report compares these two approaches, highlighting practical challenges and how Vaultiq-Session leverages `sid` to deliver robust, user-friendly session management.

<br>

## The Role of `jti`

* The `jti` claim uniquely identifies each JWT token.
* It enables token-level revocation: marking a single token as revoked by storing its `jti` in a blocklist.
* On token validation, checking the `jti` against the blocklist determines if the token is still valid.

**Limitations of `jti` for Device-Aware Revocation:**

* **No inherent device association:** The `jti` alone does not link tokens to the device or session they were issued for.
* **Complex mapping needed:** To revoke all tokens from a specific device, you must track and map every `jti` to its device.
* **Operational overhead:** Managing and querying large mappings of device → multiple `jti`s is resource-intensive.
* **User experience mismatch:** Users think in terms of devices/sessions, not individual tokens.

<br>

## The Role of `sid`

* The `sid` represents a session or device identifier.
* Multiple tokens issued to the same device/session share the same `sid`.
* Revoking a `sid` effectively revokes *all* tokens associated with that session/device.

**Advantages of `sid` for Device-Aware Session Management:**

* **Simplicity:** Blocklisting a `sid` invalidates all tokens from the session/device without tracking individual tokens.
* **Matches user expectations:** Users can easily logout from “this device,” “other devices,” or “all devices” with clear semantics.
* **Resource efficiency:** No need to store or manage many `jti`s per device.
* **Performance:** Quick revocation checks by verifying if the `sid` is blocklisted.

<br>

## Why Vaultiq-Session Uses `sid`

Vaultiq-Session centers token awareness around `sid` for practical reasons:

* **Efficient revocation:** Since all tokens from a device share the same `sid`, blocking that `sid` instantly revokes access.
* **User-friendly control:** Supports intuitive operations like “logout current device” or “logout all others” without token-level complexity.
* **Scalable design:** Avoids the overhead and confusion of mapping thousands of `jti`s per device.
* **Security-focused:** Ensures that tokens containing a blocklisted `sid` are rejected, preventing unauthorized access.

<br>

## Conclusion

While `jti` is ideal for unique token identification and individual token revocation, it falls short in device-level session control without significant additional complexity.

Using `sid` aligns better with how users think about sessions and devices, providing:

* Clear, predictable session revocation semantics.
* Lightweight and maintainable blocklist management.
* Seamless integration with device-aware authentication flows.

Vaultiq-Session’s sid-based approach is a deliberate design choice to bridge the gap between security, usability, and operational simplicity — making it a powerful tool for modern session management.

<br>

## References

* [RFC 7519 - JSON Web Token (JWT)](https://tools.ietf.org/html/rfc7519)
* Vaultiq-Session Library Documentation and Design


*This report is intended to clarify session management strategies for developers and security engineers integrating Vaultiq-Session or designing token revocation mechanisms.*
