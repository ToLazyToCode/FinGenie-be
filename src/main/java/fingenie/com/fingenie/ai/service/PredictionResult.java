package fingenie.com.fingenie.ai.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * AI Prediction result DTO.
 * 
 * Contains prediction output along with metadata
 * for auditability and transparency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * User ID this prediction is for.
     */
    private Long userId;
    
    /**
     * Predicted spending amount.
     */
    private BigDecimal predictedAmount;
    
    /**
     * Predicted spending category (optional).
     */
    private String predictedCategory;
    
    /**
     * Model confidence score (0-1).
     */
    private double confidence;
    
    /**
     * Risk score for overspending (0-1).
     */
    private double riskScore;
    
    /**
     * Model version used for prediction.
     */
    private String modelVersion;
    
    /**
     * Feature hash for reproducibility.
     */
    private String featureHash;
    
    /**
     * Correlation ID for tracing.
     */
    private String correlationId;
    
    /**
     * Inference latency in milliseconds.
     */
    private Integer inferenceLatencyMs;
    
    /**
     * Whether fallback was used.
     */
    private boolean fallbackUsed;
    
    /**
     * Reason for fallback (if applicable).
     */
    private String fallbackReason;
    
    /**
     * Explanation factors for transparency.
     */
    private Map<String, Object> explanation;
    
    /**
     * Check if prediction is reliable based on confidence.
     */
    public boolean isReliable() {
        return confidence >= 0.5 && !fallbackUsed;
    }
    
    /**
     * Check if this is a high-risk prediction.
     */
    public boolean isHighRisk() {
        return riskScore >= 0.7;
    }
}
