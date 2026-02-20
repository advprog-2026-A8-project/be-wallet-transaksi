package id.ac.ui.cs.advprog.bewallettransaksi.model;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "wallet",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "userId")
        }
)
public class Wallet {

    @Id
    @GeneratedValue
    private UUID walletId;

    @Column(nullable = false)
    private UUID userId;

    @DecimalMin(value = "0.00", inclusive = true)
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;
}
