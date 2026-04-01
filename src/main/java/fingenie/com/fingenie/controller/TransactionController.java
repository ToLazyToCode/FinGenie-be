package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.TransactionRequest;
import fingenie.com.fingenie.dto.TransactionResponse;
import fingenie.com.fingenie.dto.TransactionSuggestionResponse;
import fingenie.com.fingenie.service.TransactionService;
import fingenie.com.fingenie.service.TransactionSuggestionService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Financial transaction management - income, expenses, and savings")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionSuggestionService transactionSuggestionService;

    @PostMapping
    @Operation(
        summary = "Create a transaction",
        description = "Records a new financial transaction (INCOME, EXPENSE, or SAVING). " +
                     "Automatically updates the associated wallet balance and triggers pet mood changes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction created successfully",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data or insufficient balance"),
        @ApiResponse(responseCode = "404", description = "Wallet or category not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(request);
    }

    @GetMapping
    @Operation(
        summary = "Get all transactions",
        description = "Returns paginated transactions for the authenticated user, ordered by date descending. " +
                     "Use ?page=0&size=20&sort=transactionDate,desc for pagination."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of transactions returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public Page<TransactionResponse> getAll(
        @PageableDefault(size = 20, sort = "transactionDate", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable
    ) {
        return transactionService.getAll(pageable);
    }

    @GetMapping("/suggestions/today")
    @Operation(
        summary = "Get today's transaction suggestion",
        description = "Returns one AI suggestion for the authenticated user today, or null if no suggestion is available."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Suggestion returned (or null when none)"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<TransactionSuggestionResponse> getTodaySuggestion() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(transactionSuggestionService.getTodaySuggestion(accountId).orElse(null));
    }

    @GetMapping("/wallet/{walletId}")
    @Operation(
        summary = "Get transactions by wallet",
        description = "Returns all transactions for a specific wallet, ordered by date descending."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of transactions returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TransactionResponse.class)))),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public List<TransactionResponse> getByWallet(
        @Parameter(description = "Wallet ID", required = true, example = "1")
        @PathVariable Long walletId
    ) {
        return transactionService.getByWallet(walletId);
    }

    @GetMapping("/{transactionId}")
    @Operation(
        summary = "Get transaction by ID",
        description = "Returns a specific transaction by its ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction found",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public TransactionResponse getById(
        @Parameter(description = "Transaction ID", required = true, example = "1")
        @PathVariable Long transactionId
    ) {
        return transactionService.getById(transactionId);
    }

    @PutMapping("/{transactionId}")
    @Operation(
        summary = "Update transaction",
        description = "Updates an existing transaction. Recalculates wallet balance based on changes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction updated successfully",
            content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid transaction data"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public TransactionResponse update(
            @Parameter(description = "Transaction ID", required = true, example = "1")
            @PathVariable Long transactionId,
            @Valid @RequestBody TransactionRequest request
    ) {
        return transactionService.update(transactionId, request);
    }

    @DeleteMapping("/{transactionId}")
    @Operation(
        summary = "Delete transaction",
        description = "Deletes a transaction and reverses its effect on the wallet balance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Transaction not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> delete(
        @Parameter(description = "Transaction ID", required = true, example = "1")
        @PathVariable Long transactionId
    ) {
        transactionService.delete(transactionId);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }
}
