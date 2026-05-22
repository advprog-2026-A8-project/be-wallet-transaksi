package id.ac.ui.cs.advprog.bewallettransaksi.controller;

import id.ac.ui.cs.advprog.bewallettransaksi.config.WalletMetricsRecorder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsernameToUserIdResolverConfigTest {

    private final UsernameToUserIdResolverConfig config = new UsernameToUserIdResolverConfig();

    @Test
    void authServiceResolver_ShouldUseDefaultTimeoutWhenConfiguredTimeoutNonPositive() {
        ObjectProvider<WalletMetricsRecorder> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);

        UsernameToUserIdResolver resolver = config.authServiceUsernameToUserIdResolver(
                "http://localhost:8080",
                UsernameToUserIdResolverConfig.DEFAULT_USER_LOOKUP_PATH,
                0L,
                provider
        );

        assertInstanceOf(AuthServiceUsernameToUserIdResolver.class, resolver);
    }

    @Test
    void noopResolver_ShouldReturnNoopImplementation() {
        UsernameToUserIdResolver resolver = config.noopUsernameToUserIdResolver();

        assertInstanceOf(NoopUsernameToUserIdResolver.class, resolver);
    }
}

