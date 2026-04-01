package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.WalletRequest;
import fingenie.com.fingenie.dto.WalletResponse;
import fingenie.com.fingenie.service.WalletService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallets", description = "Wallet management - create, read, update, delete wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(
        summary = "Create a new wallet",
        description = "Creates a new wallet for the authenticated user. First wallet is automatically set as default."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet created successfully",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid wallet data"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public WalletResponse create(@Valid @RequestBody WalletRequest request) {
        return walletService.create(request);
    }

    @GetMapping
    @Operation(
        summary = "Get all wallets",
        description = "Returns all wallets belonging to the authenticated user, ordered by creation date."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of wallets returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = WalletResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public List<WalletResponse> getAll() {
        return walletService.getAll();
    }

    @GetMapping("/default")
    @Operation(
        summary = "Get default wallet",
        description = "Returns the user's default wallet. Used for quick transaction entry."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Default wallet returned",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "404", description = "No default wallet found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public WalletResponse getDefault() {
        return walletService.getDefault();
    }

    @GetMapping("/{walletId}")
    @Operation(
        summary = "Get wallet by ID",
        description = "Returns a specific wallet by its ID. Only returns wallets owned by the authenticated user."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet found",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public WalletResponse getById(
        @Parameter(description = "Wallet ID", required = true, example = "1")
        @PathVariable Long walletId
    ) {
        return walletService.getById(walletId);
    }

    @PutMapping("/{walletId}")
    @Operation(
        summary = "Update wallet",
        description = "Updates an existing wallet's name, type, currency, or default status."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet updated successfully",
            content = @Content(schema = @Schema(implementation = WalletResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid wallet data"),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public WalletResponse update(
            @Parameter(description = "Wallet ID", required = true, example = "1")
            @PathVariable Long walletId,
            @Valid @RequestBody WalletRequest request
    ) {
        return walletService.update(walletId, request);
    }

    @DeleteMapping("/{walletId}")
    @Operation(
        summary = "Delete wallet",
        description = "Deletes a wallet and all its transactions. Cannot delete the only remaining wallet."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Wallet deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Cannot delete the only wallet"),
        @ApiResponse(responseCode = "404", description = "Wallet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> delete(
        @Parameter(description = "Wallet ID", required = true, example = "1")
        @PathVariable Long walletId
    ) {
        walletService.delete(walletId);
        return ResponseEntity.ok(Map.of("message", "Wallet deleted successfully"));
    }
}
