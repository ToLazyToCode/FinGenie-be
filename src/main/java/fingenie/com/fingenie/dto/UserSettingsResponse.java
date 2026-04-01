package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * User settings response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsResponse {
    
    // Display Preferences
    private String currency;
    private String locale;
    private String theme;
    private String dateFormat;
    
    // Privacy Settings
    private Boolean showProfilePublicly;
    private Boolean showInLeaderboard;
    private Boolean allowFriendRequests;
    private Boolean showLastActive;
    
    // AI Preferences
    private Boolean allowAILearning;
    private Boolean proactiveGuessPrompts;
    private String preferredAIPersonality;
    
    // Security
    private Boolean requireBiometricForTransactions;
    private Boolean sessionTimeoutEnabled;
    private Integer sessionTimeoutMinutes;
    
    private Timestamp updatedAt;
}
