package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import fingenie.com.fingenie.entity.SharedPiggyInvitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedPiggyInvitationResponse {

    private Long id;
    private Long inviterId;
    private String inviterName;
    private String inviterAvatar;
    private Long inviteeId;
    private String inviteeName;
    private String inviteeAvatar;
    private Long walletId;
    private String piggyTitle;
    private BigDecimal goalAmount;
    private Date lockUntil;
    private SharedPiggyInvitation.Status status;
    private Long createdPiggyId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime respondedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime acceptedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private Timestamp createdAt;
}
