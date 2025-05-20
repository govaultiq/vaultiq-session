package vaultiq.session.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import vaultiq.session.config.VaultiqSessionProperties;

public class VaultiqSessionInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(VaultiqSessionInitializer.class);

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        log.debug("Initializing VaultiqSessionContext; Getting Environment...");
        var environment = applicationContext.getEnvironment();
        var binder = Binder.get(environment);

        log.debug("Binding Environment with VaultiqSessionProperties...");
        var props = binder.bind("vaultiq.session", VaultiqSessionProperties.class)
                .orElse(new VaultiqSessionProperties());

        log.debug("Resolved VaultiqSessionProperties; Creating VaultiqSessionContext...");
        var vaultiqSessionContext = new VaultiqSessionContext(props);
        VaultiqSessionContextHolder.setContext(vaultiqSessionContext);
        log.debug("VaultiqSessionContext initialized successfully.");
    }
}
