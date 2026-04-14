package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopUpRequest {

    @NotNull
    private UUID userId;

    @NotNull
    @DecimalMin(value = "1", message = "Amount must be at least 1")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    private BigDecimal amount;
}
