package vaultiq.session.context;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import vaultiq.session.config.VaultiqSessionProperties;

public class VaultiqSessionInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        var environment = applicationContext.getEnvironment();
        var binder = Binder.get(environment);

        var props = binder.bind("vaultiq.session", VaultiqSessionProperties.class)
                .orElse(new VaultiqSessionProperties());

        var vaultiqSessionContext = new VaultiqSessionContext(props);
        VaultiqSessionContextHolder.setContext(vaultiqSessionContext);
    }
}
