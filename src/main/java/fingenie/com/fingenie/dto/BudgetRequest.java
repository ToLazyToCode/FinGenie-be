package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.Budget.PeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetRequest {
    
    private Long categoryId; // null for total budget
    
    @NotNull(message = "Budget amount is required")
    @DecimalMin(value = "0.01", message = "Budget amount must be positive")
    private BigDecimal amount;
    
    @NotNull(message = "Period type is required")
    private PeriodType periodType;
    
    @Min(value = 1, message = "Alert threshold must be at least 1%")
    @Max(value = 100, message = "Alert threshold cannot exceed 100%")
    private Integer alertThreshold;
    
    private Boolean notifyOnExceed;
    
    private Boolean notifyOnWarning;
    
    private Boolean rolloverExcess;
    
    private String notes;
}
