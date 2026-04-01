package fingenie.com.fingenie.ai.service;

import fingenie.com.fingenie.ai.dto.PredictionFeedbackRequest;
import fingenie.com.fingenie.ai.event.BehaviorTrainingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PredictionFeedbackService {

    private final ApplicationEventPublisher eventPublisher;

    public void ingestFeedback(PredictionFeedbackRequest req) {
        if (req == null) return;
        if (req.getAccountId() == null || req.getPredictionId() == null) return;

        String feedbackType = req.getFeedbackType();
        if (feedbackType == null || feedbackType.isBlank()) feedbackType = "ACCEPT";

        eventPublisher.publishEvent(new BehaviorTrainingEvent(
                this,
                req.getAccountId(),
                req.getPredictionId(),
                feedbackType,
                req.getComment()
        ));
    }
}
