package fingenie.com.fingenie.ai.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class BehaviorTrainingEvent extends ApplicationEvent {

    private final Long accountId;
    private final Long predictionId;
    private final String feedbackType; // ACCEPT / REJECT / EDIT
    private final String comment;

    public BehaviorTrainingEvent(Object source, Long accountId, Long predictionId, String feedbackType, String comment) {
        super(source);
        this.accountId = accountId;
        this.predictionId = predictionId;
        this.feedbackType = feedbackType;
        this.comment = comment;
    }

}
