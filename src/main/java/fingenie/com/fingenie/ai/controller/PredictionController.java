package fingenie.com.fingenie.ai.controller;

import fingenie.com.fingenie.ai.client.AIClient;
import fingenie.com.fingenie.ai.client.dto.PredictionRequest;
import fingenie.com.fingenie.ai.dto.PredictionFeedbackRequest;
import fingenie.com.fingenie.ai.service.PredictionFeedbackService;
import fingenie.com.fingenie.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/ai/predictions")
@RequiredArgsConstructor
@Tag(name = "AI Predictions", description = "AI-powered financial predictions")
public class PredictionController {

    private final AIClient aiClient;
    private final PredictionFeedbackService feedbackService;

    @Operation(summary = "Get daily spending prediction", description = "AI predicts user's daily spending based on behavior patterns")
    @GetMapping("/daily/{userId}")
    public ResponseEntity<Map<String, Object>> getDailyPrediction(@PathVariable Long userId) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PredictionRequest request = PredictionRequest.builder()
                .accountId(accountId)
                .predictionType("DAILY")
                .build();
        Map<String, Object> prediction = aiClient.predict(request);
        return ResponseEntity.ok(prediction);
    }

    @Operation(summary = "Get category-specific prediction", description = "AI predicts spending for specific category")
    @GetMapping("/category/{userId}/{category}")
    public ResponseEntity<Map<String, Object>> getCategoryPrediction(
            @PathVariable Long userId, 
            @PathVariable String category) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        PredictionRequest request = PredictionRequest.builder()
                .accountId(accountId)
                .predictionType("CATEGORY")
                .category(category)
                .build();
        Map<String, Object> prediction = aiClient.predict(request);
        return ResponseEntity.ok(prediction);
    }

    @Operation(summary = "Submit prediction feedback", description = "User provides feedback on AI predictions for learning")
    @PostMapping("/feedback")
    public ResponseEntity<Void> submitFeedback(@RequestBody PredictionFeedbackRequest request) {
        request.setAccountId(SecurityUtils.getCurrentAccountId());
        feedbackService.ingestFeedback(request);
        return ResponseEntity.ok().build();
    }
}
