package fingenie.com.fingenie.admin.dto;

import fingenie.com.fingenie.entity.Account;
import lombok.Data;

/**
 * Partial update payload for a user account.
 * All fields are optional – only non-null values are applied.
 */
@Data
public class AdminUpdateUserRequest {

    /** Display name to update. */
    private String name;

    /** Change the user's role (USER | MODERATOR | ADMIN). */
    private Account.Role role;

    /** Grant or revoke premium status. */
    private Boolean isPremium;

    /** Mark the user's email as verified (KYC override). */
    private Boolean emailVerified;
}
