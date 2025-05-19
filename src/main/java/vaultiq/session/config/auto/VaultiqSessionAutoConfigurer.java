package vaultiq.session.config.auto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.core.util.VaultiqSessionContext;

@Configuration
@ComponentScan(basePackages = {
        "vaultiq.session.cache",
        "vaultiq.session.config",
        "vaultiq.session.core",
        "vaultiq.session.fingerprint",
})
@EnableConfigurationProperties(VaultiqSessionProperties.class)
public class VaultiqSessionAutoConfigurer {

    @Bean
    VaultiqSessionContext vaultiqSessionContext(VaultiqSessionProperties props) {
        return new VaultiqSessionContext(props);
    }
}
