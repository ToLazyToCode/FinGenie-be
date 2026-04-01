package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSystemHealthResponse {

    /** Overall system status: UP, DEGRADED, DOWN */
    private String status;

    /** Database connectivity status */
    private String databaseStatus;

    /** JVM heap used in megabytes */
    private long heapUsedMb;

    /** JVM heap max in megabytes */
    private long heapMaxMb;

    /** JVM heap usage as percentage 0-100 */
    private double heapUsagePercent;

    /** Number of active JVM threads */
    private int activeThreads;

    /** Application uptime in seconds */
    private long uptimeSeconds;
}
