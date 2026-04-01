package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class CreateSharedPiggyInvitationRequest {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotNull(message = "Invitee ID is required")
    private Long inviteeId;

    @NotBlank(message = "Piggy title is required")
    private String piggyTitle;

    @NotNull(message = "Goal amount is required")
    @Positive(message = "Goal amount must be greater than 0")
    private BigDecimal goalAmount;

    private Date lockUntil;
}
