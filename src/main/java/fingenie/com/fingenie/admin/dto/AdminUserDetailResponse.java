package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Full user detail view – extends AdminUserResponse with extra analytics fields.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdminUserDetailResponse extends AdminUserResponse {

    /** Profile / avatar picture URL (from Account.avatarUrl). */
    private String profilePicture;

    /** Alias for createdAt – when the user joined. */
    private Timestamp joinDate;

    /** Timestamp of the user's last recorded login (Account.lastLogin). */
    private Timestamp lastActivityAt;

    /** Number of abuse/fraud reports filed against this user (placeholder – no report entity yet). */
    private long reportsCount;

    /** Number of admin warnings issued to this user (placeholder). */
    private long warningsCount;

    /** Absolute sum of all expense transactions (amount < 0). */
    private BigDecimal totalSpent;

    /** Sum of all income transactions (amount > 0). */
    private BigDecimal totalEarned;
}
