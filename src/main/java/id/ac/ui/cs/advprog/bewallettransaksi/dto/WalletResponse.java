package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WalletResponse {

    private final UUID walletId;
    private final UUID userId;
    private final BigDecimal balance;
}
