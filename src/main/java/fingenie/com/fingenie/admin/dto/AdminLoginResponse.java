package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String accessToken;
    private String tokenType;
    private Long expiresIn;

    private Long adminId;
    private String email;
    private String name;
    private String role;
}
