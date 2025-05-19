package vaultiq.session.config.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {
        "vaultiq.session.cache",
        "vaultiq.session.config",
        "vaultiq.session.core",
        "vaultiq.session.fingerprint",
})
public class VaultiqSessionAutoConfigurer {

    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionAutoConfigurer.class);

    public VaultiqSessionAutoConfigurer() {
        log.info("Initializing VaultiqSessionAutoConfigurer 🚀");
    }
}
