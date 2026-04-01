package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoAllocatePolicyRequest {

    @NotNull(message = "enabled is required")
    private Boolean enabled;
}
