package fingenie.com.fingenie.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceResponse {

    private Boolean pushEnabled;
    private Boolean emailEnabled;
    private Boolean budgetAlerts;
    private Boolean streakReminders;
    private Boolean guessPrompts;
    private Boolean goalProgress;
    private Boolean friendActivity;
    private Boolean achievementUnlocks;
    private Boolean dailySummary;
}
