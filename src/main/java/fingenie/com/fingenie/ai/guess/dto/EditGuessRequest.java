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
public class EditGuessRequest {
    private BigDecimal amount;
    private Long categoryId;
    private Long walletId;
    private String description;
}
