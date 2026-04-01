package fingenie.com.fingenie.ai.guess.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptGuessResponse {
    private Long guessId;
    private Long transactionId;
    private BigDecimal amount;
    private String category;
    private String message;
}
