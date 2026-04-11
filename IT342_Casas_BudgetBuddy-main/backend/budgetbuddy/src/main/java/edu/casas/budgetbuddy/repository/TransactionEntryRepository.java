package edu.casas.budgetbuddy.repository;

import edu.casas.budgetbuddy.entity.TransactionEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionEntryRepository extends JpaRepository<TransactionEntry, Long> {

    List<TransactionEntry> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
