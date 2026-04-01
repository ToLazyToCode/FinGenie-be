package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * Lightweight user record used in the paginated admin user list.
 * Status is derived server-side:
 *   ACTIVE       → isActive=true  && !isDeleted && emailVerified=true
 *   PENDING_KYC  → isActive=true  && !isDeleted && emailVerified=false
 *   INACTIVE     → isActive=true  && isDeleted=true
 *   BANNED       → isActive=false
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponse {

    private Long id;
    private String email;
    private String name;

    /** Phone number sourced from UserProfile (may be null). */
    private String phone;

    /** Derived status: ACTIVE | INACTIVE | BANNED | PENDING_KYC */
    private String status;

    /** KYC status derived from emailVerified: VERIFIED | PENDING */
    private String kycStatus;

    private Timestamp createdAt;
    private Timestamp lastLogin;

    private long totalTransactions;

    /** Sum of all wallet balances for this user. */
    private BigDecimal totalBalance;
}
