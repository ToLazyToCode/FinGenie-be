package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PiggyMemberShareWeightRequest {

    @NotNull(message = "Share weight is required")
    @Min(value = 1, message = "Share weight must be at least 1")
    private Integer shareWeight;
}
