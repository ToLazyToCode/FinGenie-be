package fingenie.com.fingenie.ai.guess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingGuessResponse {
    private Long id;
    private BigDecimal amount;
    private String currency;
    private String category;
    private Long categoryId;
    private String walletName;
    private Long walletId;
    private double confidence;
    private String reasoning;
    private LocalDateTime guessedForTime;
    private LocalDateTime expiresAt;
    private String status;
}
