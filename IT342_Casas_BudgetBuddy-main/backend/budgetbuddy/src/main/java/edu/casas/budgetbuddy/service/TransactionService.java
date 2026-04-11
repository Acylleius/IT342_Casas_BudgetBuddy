package edu.casas.budgetbuddy.service;

import edu.casas.budgetbuddy.dto.TransactionDashboardDto;
import edu.casas.budgetbuddy.dto.TransactionDto;
import edu.casas.budgetbuddy.dto.TransactionSummaryDto;
import edu.casas.budgetbuddy.dto.TransactionRequest;

public interface TransactionService {

    TransactionDto create(TransactionRequest request);

    TransactionDashboardDto getDashboard(Long userId);

    TransactionSummaryDto getSummary(Long userId);

    void delete(Long transactionId, Long userId);
}
