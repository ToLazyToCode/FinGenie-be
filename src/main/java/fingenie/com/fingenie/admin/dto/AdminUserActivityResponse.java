package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * One login-activity record for a user.
 * NOTE: A persistent LoginLog entity does not yet exist in the schema.
 * Until it is added, the /activity endpoint returns an empty page.
 * Fields here match the intended table shape so the frontend contract is stable.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserActivityResponse {

    private Long loginId;
    private Timestamp loginTime;
    private String ipAddress;
    private String deviceInfo;
    private String userAgent;

    /** "SUCCESS" or "FAILED" */
    private String status;
}
