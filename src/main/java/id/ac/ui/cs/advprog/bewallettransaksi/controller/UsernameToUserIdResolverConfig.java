package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UsernameToUserIdResolverConfig {
    static final String DEFAULT_USER_LOOKUP_PATH = "/api/profile/lookup";

    @Bean
    @ConditionalOnProperty(
            name = "auth.service.username-resolver.enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    UsernameToUserIdResolver authServiceUsernameToUserIdResolver(
            @Value("${auth.service.base-url:http://localhost:8080}") String baseUrl,
            @Value("${auth.service.user-lookup-path:" + DEFAULT_USER_LOOKUP_PATH + "}") String userLookupPath,
            @Value("${auth.service.timeout-ms:1000}") long timeoutMs
    ) {
        long safeTimeoutMs = timeoutMs > 0 ? timeoutMs : 1000;
        return new AuthServiceUsernameToUserIdResolver(
                baseUrl,
                userLookupPath,
                java.net.http.HttpClient.newHttpClient(),
                Duration.ofMillis(safeTimeoutMs)
        );
    }

    @Bean
    @ConditionalOnMissingBean(UsernameToUserIdResolver.class)
    UsernameToUserIdResolver noopUsernameToUserIdResolver() {
        return new NoopUsernameToUserIdResolver();
    }
}
