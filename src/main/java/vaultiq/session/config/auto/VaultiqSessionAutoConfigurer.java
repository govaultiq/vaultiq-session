package vaultiq.session.config.auto;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(VaultiqSessionContextAutoConfigurer.class)
@ComponentScan(basePackages = {
        "vaultiq.session.cache",
        "vaultiq.session.config",
        "vaultiq.session.core",
        "vaultiq.session.fingerprint",
})
public class VaultiqSessionAutoConfigurer {

}
