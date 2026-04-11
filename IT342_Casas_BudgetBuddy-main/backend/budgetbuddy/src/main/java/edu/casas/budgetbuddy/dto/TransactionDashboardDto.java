package edu.casas.budgetbuddy.dto;

import java.util.List;

public class TransactionDashboardDto {

    private final TransactionSummaryDto summary;
    private final List<TransactionDto> transactions;

    public TransactionDashboardDto(TransactionSummaryDto summary, List<TransactionDto> transactions) {
        this.summary = summary;
        this.transactions = transactions;
    }

    public TransactionSummaryDto getSummary() {
        return summary;
    }

    public List<TransactionDto> getTransactions() {
        return transactions;
    }
}
