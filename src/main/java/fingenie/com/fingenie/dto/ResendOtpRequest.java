package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendOtpRequest {

    @NotBlank(message = "Session ID is required")
    private String sessionId;
}
