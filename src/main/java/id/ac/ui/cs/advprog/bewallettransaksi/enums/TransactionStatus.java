package id.ac.ui.cs.advprog.bewallettransaksi.enums;

import lombok.Getter;

@Getter
public enum TransactionStatus {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED");

    private final String value;

    private TransactionStatus(String value) {
        this.value = value;
    }

    public static boolean contains(String param) {
        for (TransactionStatus status : TransactionStatus.values()) {
            if (status.name().equals(param)) {
                return true;
            }
        }
        return false;
    }
}