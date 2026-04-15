package id.ac.ui.cs.advprog.bewallettransaksi.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class WalletMutationRequest {
    private static final String USER_ID_REQUIRED_MESSAGE = "User ID must not be null";
    private static final String DESCRIPTION_REQUIRED_MESSAGE = "Description must not be blank";

    @NotNull(message = USER_ID_REQUIRED_MESSAGE)
    private UUID userId;

    @NotNull
    @DecimalMin(value = AmountValidationConstants.MIN_AMOUNT, message = AmountValidationConstants.MIN_AMOUNT_MESSAGE)
    @Digits(
            integer = AmountValidationConstants.MAX_INTEGER_DIGITS,
            fraction = AmountValidationConstants.MAX_FRACTION_DIGITS,
            message = AmountValidationConstants.MAX_FRACTION_MESSAGE
    )
    private BigDecimal amount;

    @NotBlank(message = DESCRIPTION_REQUIRED_MESSAGE)
    private String description;
}
