package id.ac.ui.cs.advprog.bewallettransaksi.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionType;
import id.ac.ui.cs.advprog.bewallettransaksi.model.state.TransactionState;
import id.ac.ui.cs.advprog.bewallettransaksi.model.state.TransactionStateFactory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    private UUID transactionId;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(TransactionStatus nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Transaction status must not be null");
        }

        if (this.status != null && nextStatus != null) {
            TransactionState currentState = TransactionStateFactory.from(this.status);
            if (!currentState.canTransitionTo(nextStatus)) {
                throw new IllegalStateException(
                        "Invalid transaction status transition: " + this.status + " -> " + nextStatus
                );
            }
        }
        this.status = nextStatus;
    }
}
