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

    @NotNull(message = AmountValidationConstants.USER_ID_REQUIRED_MESSAGE)
    private UUID userId;

    @NotNull(message = AmountValidationConstants.MIN_AMOUNT_MESSAGE)
    @DecimalMin(value = AmountValidationConstants.MIN_AMOUNT, message = AmountValidationConstants.MIN_AMOUNT_MESSAGE)
    @Digits(
            integer = AmountValidationConstants.MAX_INTEGER_DIGITS,
            fraction = AmountValidationConstants.MAX_FRACTION_DIGITS,
            message = AmountValidationConstants.MAX_FRACTION_MESSAGE
    )
    private BigDecimal amount;
}
