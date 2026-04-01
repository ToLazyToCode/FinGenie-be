package fingenie.com.fingenie.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardChartsResponse {

    /**
     * New user registrations grouped by day.
     * Key = date string (e.g. "2025-01-15"), Value = count.
     */
    private Map<String, Long> userGrowthByDay;

    /**
     * Transaction volume grouped by day.
     * Key = date string, Value = count.
     */
    private Map<String, Long> transactionVolumeByDay;

    /**
     * Labels for the chart x-axis (ordered date strings).
     */
    private List<String> labels;
}
