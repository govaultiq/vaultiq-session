package vaultiq.session.jpa.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vaultiq.session.cache.model.ModelType;
import vaultiq.session.config.annotation.ConditionalOnVaultiqModelConfig;
import vaultiq.session.config.annotation.model.VaultiqPersistenceMethod;

/**
 * Spring Boot autoconfiguration for enabling JPA persistence specifically for the
 * Vaultiq session blocklist model.
 * <p>
 * This configuration is active only when the Vaultiq session persistence is
 * explicitly configured to use JPA for the {@link ModelType#BLOCKLIST} via
 * the {@link ConditionalOnVaultiqModelConfig} annotation.
 * </p>
 * <p>
 * When active, it automatically configures:
 * <ul>
 * <li>JPA entity scanning for blocklist-related entities.</li>
 * <li>JPA repository scanning for blocklist repositories.</li>
 * <li>Component scanning for JPA-based blocklist services.</li>
 * </ul>
 * </p>
 *
 * @see ConditionalOnVaultiqModelConfig
 * @see VaultiqPersistenceMethod#USE_JPA
 * @see ModelType#BLOCKLIST
 * @see EntityScan
 * @see EnableJpaRepositories
 * @see ComponentScan
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnVaultiqModelConfig(
        method = VaultiqPersistenceMethod.USE_JPA,
        type = ModelType.BLOCKLIST
)
@EntityScan("vaultiq.session.jpa.blocklist.model")
@EnableJpaRepositories("vaultiq.session.jpa.blocklist.repository")
@ComponentScan(basePackages = "vaultiq.session.jpa.blocklist")
public class SessionBlocklistAutoConfigurationJpa {

    private static final Logger log = LoggerFactory.getLogger(SessionBlocklistAutoConfigurationJpa.class);

    public SessionBlocklistAutoConfigurationJpa() {
        log.info("Initializing SessionBlocklistAutoConfigurationJpa 📦");
    }
}
