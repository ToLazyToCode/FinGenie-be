package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User settings/preferences request for updates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {
    
    // Display Preferences
    private String currency; // USD, EUR, GBP, etc.
    private String locale; // en-US, vi-VN, etc.
    private String theme; // light, dark, auto
    private String dateFormat; // MM/dd/yyyy, dd/MM/yyyy, etc.
    
    // Privacy Settings
    private Boolean showProfilePublicly;
    private Boolean showInLeaderboard;
    private Boolean allowFriendRequests;
    private Boolean showLastActive;
    
    // AI Preferences
    private Boolean allowAILearning;
    private Boolean proactiveGuessPrompts;
    private String preferredAIPersonality; // friendly, professional, playful
    
    // Security
    private Boolean requireBiometricForTransactions;
    private Boolean sessionTimeout; // auto logout after inactivity
    private Integer sessionTimeoutMinutes;
}
