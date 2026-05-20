package id.ac.ui.cs.advprog.bewallettransaksi;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class BeWalletTransaksiApplicationTest {

    @Test
    void main_ShouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            springApplication.when(() -> SpringApplication.run(eq(BeWalletTransaksiApplication.class), eq(new String[]{"arg1"})))
                    .thenReturn(mock(ConfigurableApplicationContext.class));

            BeWalletTransaksiApplication.main(new String[]{"arg1"});

            springApplication.verify(() -> SpringApplication.run(eq(BeWalletTransaksiApplication.class), eq(new String[]{"arg1"})));
        }
    }
}
