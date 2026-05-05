package id.ac.ui.cs.advprog.bewallettransaksi.repository;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import jakarta.persistence.LockModeType;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Transaction t WHERE t.transactionId = :transactionId")
    java.util.Optional<Transaction> findByIdForUpdate(@Param("transactionId") UUID transactionId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Transaction t
            SET t.status = :nextStatus,
                t.updatedAt = :updatedAt
            WHERE t.transactionId = :transactionId
              AND t.status = :expectedStatus
            """)
    int transitionStatusIfMatches(
            @Param("transactionId") UUID transactionId,
            @Param("expectedStatus") TransactionStatus expectedStatus,
            @Param("nextStatus") TransactionStatus nextStatus,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.walletId = :walletId
            ORDER BY t.createdAt DESC, t.transactionId DESC
            """)
    List<Transaction> findByWalletIdOrderByCreatedAtDesc(@Param("walletId") UUID walletId);

    @Query("""
            SELECT t
            FROM Transaction t
            WHERE t.walletId = :walletId
              AND t.status = :status
            ORDER BY t.createdAt DESC, t.transactionId DESC
            """)
    List<Transaction> findByWalletIdAndStatusOrderByCreatedAtDesc(@Param("walletId") UUID walletId,
                                                                   @Param("status") TransactionStatus status);
}
