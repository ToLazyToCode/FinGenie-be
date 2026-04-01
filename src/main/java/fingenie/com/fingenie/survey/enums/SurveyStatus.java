package fingenie.com.fingenie.survey.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Survey response status tracking.
 */
@Getter
@RequiredArgsConstructor
public enum SurveyStatus {
    NOT_STARTED("Not Started"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    EXPIRED("Expired"),
    SUPERSEDED("Superseded by new version");
    
    private final String displayName;
}
