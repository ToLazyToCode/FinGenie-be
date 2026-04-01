package fingenie.com.fingenie.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PetRequest {
    private Long accountId;
    private String petName;
    private String petType;
    private String petAvatarUrl;
    private String personality;
}
