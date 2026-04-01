package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetResponse {
    private Long petId;
    private Long accountId;
    private String petName;
    private String petType;
    private String petAvatarUrl;
    private Integer level;
    private Integer experiencePoints;
    private String mood;
    private String personality;
}
