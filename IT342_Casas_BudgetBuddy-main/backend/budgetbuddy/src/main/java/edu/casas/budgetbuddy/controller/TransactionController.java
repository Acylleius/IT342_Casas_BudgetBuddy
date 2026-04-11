package edu.casas.budgetbuddy.controller;

import edu.casas.budgetbuddy.dto.AppResponse;
import edu.casas.budgetbuddy.dto.TransactionDashboardDto;
import edu.casas.budgetbuddy.dto.TransactionDto;
import edu.casas.budgetbuddy.dto.TransactionRequest;
import edu.casas.budgetbuddy.dto.TransactionSummaryDto;
import edu.casas.budgetbuddy.factory.AppResponseFactory;
import edu.casas.budgetbuddy.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<AppResponse<TransactionDashboardDto>> getDashboard(@RequestParam Long userId) {
        return ResponseEntity.ok(
                AppResponseFactory.success("Transactions retrieved successfully.", transactionService.getDashboard(userId))
        );
    }

    @GetMapping("/summary")
    public ResponseEntity<AppResponse<TransactionSummaryDto>> getSummary(@RequestParam Long userId) {
        return ResponseEntity.ok(
                AppResponseFactory.success("Transaction summary retrieved successfully.", transactionService.getSummary(userId))
        );
    }

    @PostMapping
    public ResponseEntity<AppResponse<TransactionDto>> create(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AppResponseFactory.success("Transaction saved successfully.", transactionService.create(request)));
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<AppResponse<Void>> delete(@PathVariable Long transactionId, @RequestParam Long userId) {
        transactionService.delete(transactionId, userId);
        return ResponseEntity.ok(AppResponseFactory.success("Transaction deleted successfully.", null));
    }
}
