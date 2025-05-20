package vaultiq.session.config.auto;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import vaultiq.session.config.VaultiqSessionProperties;
import vaultiq.session.context.VaultiqSessionContext;
import vaultiq.session.context.VaultiqSessionContextHolder;

import java.util.Optional;

@Configuration
@ComponentScan(basePackages = "vaultiq.session.context")
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(VaultiqSessionProperties.class)
public class VaultiqSessionContextAutoConfigurer {

    @Bean
    VaultiqSessionContext vaultiqSessionContext(VaultiqSessionProperties props){
        return Optional.ofNullable(VaultiqSessionContextHolder.getContext())
                .orElse(new VaultiqSessionContext(props));
    }
}
