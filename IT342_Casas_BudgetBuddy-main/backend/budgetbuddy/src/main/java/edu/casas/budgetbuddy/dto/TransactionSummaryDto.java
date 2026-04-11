package edu.casas.budgetbuddy.dto;

import java.math.BigDecimal;

public class TransactionSummaryDto {

    private final BigDecimal totalIncome;
    private final BigDecimal totalExpense;
    private final BigDecimal balance;
    private final long transactionCount;

    public TransactionSummaryDto(BigDecimal totalIncome, BigDecimal totalExpense, BigDecimal balance, long transactionCount) {
        this.totalIncome = totalIncome;
        this.totalExpense = totalExpense;
        this.balance = balance;
        this.transactionCount = transactionCount;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpense() {
        return totalExpense;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public long getTransactionCount() {
        return transactionCount;
    }
}
