package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TransactionResponse {

    private final UUID transactionId;
    private final UUID walletId;
    private final BigDecimal amount;
    private final TransactionType type;
    private final TransactionStatus status;
    private final String description;
    private final LocalDateTime createdAt;
}
