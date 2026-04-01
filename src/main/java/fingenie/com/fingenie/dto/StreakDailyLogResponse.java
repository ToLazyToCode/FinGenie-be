package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StreakDailyLogResponse {
    private Long logId;
    private Long accountId;
    private LocalDate logDate;
    private Boolean hasTransaction;
}

