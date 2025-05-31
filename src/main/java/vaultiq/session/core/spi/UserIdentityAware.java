package vaultiq.session.core.spi;

/**
 * A functional interface defining a contract for retrieving the unique identifier
 * of the currently authenticated user.
 * <p>
 * This interface serves as an extension point for applications integrating the
 * Vaultiq session tracking library. It allows the library to identify the user
 * associated with the current execution context (e.g., from a security context,
 * request attributes, etc.) to associate session activities and data with the
 * correct user.
 * </p>
 *
 * <pre>{@code
 * // Example implementation using Spring Security
 * import org.springframework.security.core.context.SecurityContextHolder;
 * import org.springframework.stereotype.Component;
 * import vaultiq.session.core.contracts.UserIdentityAware;
 *
 * @Component
 * public class SpringSecurityUserIdentityAware implements UserIdentityAware {
 *
 * @Override
 * public String getCurrentUserID() {
 *      return SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
 *  }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface UserIdentityAware {
    /**
     * Retrieves the unique identifier of the currently authenticated user.
     * <p>
     * The implementation should fetch the user ID from the application's
     * security context or equivalent mechanism that holds information about
     * the principal performing the current operation.
     * </p>
     *
     * @return The unique string identifier of the current user. Should return
     * a non-null value representing an anonymous or system user if
     * no specific user is authenticated in the current context,
     * depending on the application's requirements.
     */
    String getCurrentUserID();
}
