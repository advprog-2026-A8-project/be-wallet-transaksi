package id.ac.ui.cs.advprog.bewallettransaksi.dto;

public final class AmountValidationConstants {
    private AmountValidationConstants() {
        // utility class
    }

    public static final String USER_ID_REQUIRED_MESSAGE = "User ID must not be null";
    public static final String DESCRIPTION_REQUIRED_MESSAGE = "Description must not be blank";
    public static final String MIN_AMOUNT = "1";
    public static final String MIN_AMOUNT_MESSAGE = "Amount must be at least 1";
    public static final int MAX_INTEGER_DIGITS = 17;
    public static final int MAX_FRACTION_DIGITS = 2;
    public static final String MAX_FRACTION_MESSAGE = "Amount must have at most 2 decimal places";
}
