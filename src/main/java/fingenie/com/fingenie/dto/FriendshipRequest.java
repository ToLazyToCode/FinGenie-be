package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FriendshipRequest {
    
    @NotNull(message = "Addressee ID is required")
    private Long addresseeId;
}
