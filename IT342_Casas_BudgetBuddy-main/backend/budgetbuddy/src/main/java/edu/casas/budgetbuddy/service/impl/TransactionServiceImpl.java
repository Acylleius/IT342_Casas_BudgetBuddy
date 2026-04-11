package edu.casas.budgetbuddy.service.impl;

import edu.casas.budgetbuddy.dto.TransactionDashboardDto;
import edu.casas.budgetbuddy.dto.TransactionDto;
import edu.casas.budgetbuddy.dto.TransactionRequest;
import edu.casas.budgetbuddy.dto.TransactionSummaryDto;
import edu.casas.budgetbuddy.entity.TransactionEntry;
import edu.casas.budgetbuddy.entity.TransactionType;
import edu.casas.budgetbuddy.entity.User;
import edu.casas.budgetbuddy.repository.TransactionEntryRepository;
import edu.casas.budgetbuddy.service.TransactionService;
import edu.casas.budgetbuddy.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionEntryRepository transactionEntryRepository;
    private final UserService userService;

    public TransactionServiceImpl(TransactionEntryRepository transactionEntryRepository, UserService userService) {
        this.transactionEntryRepository = transactionEntryRepository;
        this.userService = userService;
    }

    @Override
    public TransactionDto create(TransactionRequest request) {
        User user = userService.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        TransactionEntry entry = new TransactionEntry();
        entry.setUser(user);
        entry.setType(parseType(request.getType()));
        entry.setAmount(request.getAmount());
        entry.setCategory(request.getCategory().trim());
        entry.setDescription(request.getDescription() == null ? null : request.getDescription().trim());
        entry.setTransactionDate(request.getTransactionDate());
        entry.setCreatedAt(LocalDateTime.now());

        return TransactionDto.from(transactionEntryRepository.save(entry));
    }

    @Override
    public TransactionDashboardDto getDashboard(Long userId) {
        List<TransactionDto> transactions = transactionEntryRepository
                .findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId)
                .stream()
                .map(TransactionDto::from)
                .toList();

        return new TransactionDashboardDto(getSummary(userId), transactions);
    }

    @Override
    public TransactionSummaryDto getSummary(Long userId) {
        List<TransactionEntry> transactions = transactionEntryRepository.findByUserIdOrderByTransactionDateDescCreatedAtDesc(userId);

        BigDecimal totalIncome = transactions.stream()
                .filter(entry -> entry.getType() == TransactionType.INCOME)
                .map(TransactionEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = transactions.stream()
                .filter(entry -> entry.getType() == TransactionType.EXPENSE)
                .map(TransactionEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TransactionSummaryDto(
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense),
                transactions.size()
        );
    }

    @Override
    public void delete(Long transactionId, Long userId) {
        if (!transactionEntryRepository.existsByIdAndUserId(transactionId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found.");
        }

        transactionEntryRepository.deleteByIdAndUserId(transactionId, userId);
    }

    private TransactionType parseType(String rawType) {
        try {
            return TransactionType.valueOf(rawType.trim().toUpperCase());
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction type must be INCOME or EXPENSE.");
        }
    }
}
