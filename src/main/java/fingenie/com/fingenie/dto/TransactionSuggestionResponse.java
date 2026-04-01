package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionSuggestionResponse {
    private Long predictionId;
    private String transactionType;
    private String type;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private String note;
    private String reason;
    private Double confidence;
}

