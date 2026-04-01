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
public class SavingCapacityResponse {

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpense;
    private BigDecimal savingCapacity;
    private BigDecimal expenseRatio;
    private ConfidenceLevel confidence;

    public enum ConfidenceLevel {
        LOW,
        MEDIUM,
        HIGH
    }
}
