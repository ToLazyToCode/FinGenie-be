package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.Friendship;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipResponse {
    private Long friendshipId;
    private Long requesterId;
    private String requesterEmail;
    private Long addresseeId;
    private String addresseeEmail;
    private Friendship.FriendshipStatus status;
    private Timestamp createdAt;
}
