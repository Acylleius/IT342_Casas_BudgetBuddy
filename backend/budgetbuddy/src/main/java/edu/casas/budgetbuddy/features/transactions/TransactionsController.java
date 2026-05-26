package edu.casas.budgetbuddy.features.transactions;

import edu.casas.budgetbuddy.features.auth.AuthService;
import edu.casas.budgetbuddy.features.transactions.TransactionsDtos.TransactionRequest;
import edu.casas.budgetbuddy.shared.store.BudgetBuddyStore.UserRecord;
import edu.casas.budgetbuddy.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionsController {
    private final AuthService authService;
    private final TransactionsService transactionsService;

    public TransactionsController(AuthService authService, TransactionsService transactionsService) {
        this.authService = authService;
        this.transactionsService = transactionsService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionsDtos.TransactionDto>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody TransactionRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ResponseEntity.status(201).body(ApiResponse.success(transactionsService.create(user.id(), request.type(), request.amount(),
                request.category(), request.description(), request.transactionDate()), "Transaction saved"));
    }

    @GetMapping
    public ApiResponse<?> list(@RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(transactionsService.list(user.id()), "Transactions loaded");
    }

    @GetMapping("/summary")
    public ApiResponse<TransactionsDtos.SummaryDto> summary(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(transactionsService.summary(user.id()), "Summary loaded");
    }

    @PutMapping("/{transactionId}")
    public ApiResponse<TransactionsDtos.TransactionDto> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionRequest request) {
        UserRecord user = authService.requireUser(authorization);
        return ApiResponse.success(transactionsService.update(user.id(), transactionId, request.type(),
                request.amount(), request.category(), request.description(), request.transactionDate()),
                "Transaction updated");
    }

    @DeleteMapping("/{transactionId}")
    public ApiResponse<Void> delete(@RequestHeader(value = "Authorization", required = false) String authorization,
                                    @PathVariable Long transactionId) {
        UserRecord user = authService.requireUser(authorization);
        transactionsService.softDelete(user.id(), transactionId);
        return ApiResponse.success(null, "Transaction deleted");
    }
}
