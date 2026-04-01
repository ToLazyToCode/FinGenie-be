package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StreakDailyLogRequest {
    private Long accountId;
    private LocalDate logDate;
    private Boolean hasTransaction;
}