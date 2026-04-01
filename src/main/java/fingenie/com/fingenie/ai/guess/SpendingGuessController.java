package fingenie.com.fingenie.ai.guess;

import fingenie.com.fingenie.ai.guess.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/ai/guess")
@RequiredArgsConstructor
@Tag(name = "AI Spending Guess", description = "AI-powered spending prediction and one-tap transaction creation")
public class SpendingGuessController {

    private final SpendingGuessService guessService;

    @Operation(
        summary = "Get today's spending guesses",
        description = "Returns all pending AI-generated spending predictions for the current user today"
    )
    @ApiResponse(responseCode = "200", description = "List of pending guesses")
    @GetMapping("/today")
    public ResponseEntity<List<SpendingGuessResponse>> getTodayGuesses() {
        return ResponseEntity.ok(guessService.getTodayGuesses());
    }

    @Operation(
        summary = "Accept a spending guess",
        description = "Accepts the AI prediction and automatically creates a transaction with the guessed values"
    )
    @ApiResponse(responseCode = "200", description = "Transaction created successfully")
    @ApiResponse(responseCode = "400", description = "Guess already processed or expired")
    @ApiResponse(responseCode = "404", description = "Guess not found")
    @PostMapping("/{guessId}/accept")
    public ResponseEntity<AcceptGuessResponse> acceptGuess(@PathVariable Long guessId) {
        return ResponseEntity.ok(guessService.acceptGuess(guessId));
    }

    @Operation(
        summary = "Accept guess with edits",
        description = "Accept the prediction but modify amount, category, or wallet before creating transaction"
    )
    @ApiResponse(responseCode = "200", description = "Transaction created with modifications")
    @ApiResponse(responseCode = "400", description = "Guess already processed or expired")
    @PostMapping("/{guessId}/edit")
    public ResponseEntity<AcceptGuessResponse> acceptWithEdit(
            @PathVariable Long guessId,
            @RequestBody EditGuessRequest request) {
        return ResponseEntity.ok(guessService.acceptWithEdit(guessId, request));
    }

    @Operation(
        summary = "Reject a spending guess",
        description = "Reject the AI prediction with optional feedback reason for AI learning"
    )
    @ApiResponse(responseCode = "200", description = "Guess rejected successfully")
    @ApiResponse(responseCode = "400", description = "Guess already processed or expired")
    @PostMapping("/{guessId}/reject")
    public ResponseEntity<Map<String, String>> rejectGuess(
            @PathVariable Long guessId,
            @RequestBody(required = false) RejectGuessRequest request) {
        guessService.rejectGuess(guessId, request);
        return ResponseEntity.ok(Map.of("message", "Guess rejected successfully"));
    }
}
