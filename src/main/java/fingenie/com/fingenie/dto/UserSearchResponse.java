package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResponse {
    private Long userId;
    private String email;
    private String name;
    private String avatarUrl;
    private boolean isFriend;
    private boolean hasPendingRequest;
}
