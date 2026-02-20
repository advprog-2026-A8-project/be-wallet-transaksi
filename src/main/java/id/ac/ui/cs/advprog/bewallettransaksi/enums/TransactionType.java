package id.ac.ui.cs.advprog.bewallettransaksi.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    TOPUP("TOPUP"),
    PAYMENT("PAYMENT"),
    REFUND("REFUND"),
    WITHDRAW("WITHDRAW");

    private final String value;

    private TransactionType(String value) {
        this.value = value;
    }

    public static boolean contains(String param) {
        for (TransactionType type : TransactionType.values()) {
            if (type.name().equals(param)) {
                return true;
            }
        }
        return false;
    }
}