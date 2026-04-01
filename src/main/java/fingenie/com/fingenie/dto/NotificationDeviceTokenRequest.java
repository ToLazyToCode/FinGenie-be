package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeviceTokenRequest {

    @NotBlank
    private String deviceToken;

    private String platform;

    @Builder.Default
    private Boolean enabled = true;
}
