package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import java.math.BigDecimal;
import java.util.UUID;

public record CheckBalanceRequest(UUID userId, BigDecimal amount) {
}

