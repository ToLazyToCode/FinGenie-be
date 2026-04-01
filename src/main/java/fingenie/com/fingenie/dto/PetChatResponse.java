package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetChatResponse {
    private String petMessage;
    private String mood;
    private int happiness;
    private double confidence;
    private String personality;
}
