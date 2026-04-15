package id.ac.ui.cs.advprog.bewallettransaksi.repository;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import id.ac.ui.cs.advprog.bewallettransaksi.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
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
