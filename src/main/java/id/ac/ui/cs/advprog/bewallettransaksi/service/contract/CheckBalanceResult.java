package id.ac.ui.cs.advprog.bewallettransaksi.service.contract;

import java.math.BigDecimal;

public record CheckBalanceResult(boolean sufficient, BigDecimal currentBalance) {
}

