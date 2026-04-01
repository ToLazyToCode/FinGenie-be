package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for PetProfile entity responses.
 * OSIV-SAFE: Fully serializable without Hibernate session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetProfileResponse {

    private Long id;
    private Long accountId;
    private Integer mood;        // 0-100
    private Integer energy;      // 0-100
    private Integer hunger;      // 0-100
    private Integer happiness;   // 0-100

    /**
     * Factory method to convert entity to DTO within transactional boundary.
     */
    public static PetProfileResponse fromEntity(fingenie.com.fingenie.entity.PetProfile entity) {
        if (entity == null) {
            return null;
        }
        return PetProfileResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccountId())
                .mood(entity.getMood())
                .energy(entity.getEnergy())
                .hunger(entity.getHunger())
                .happiness(entity.getHappiness())
                .build();
    }

    /**
     * Create a default profile DTO for when no profile exists.
     */
    public static PetProfileResponse defaultProfile(Long accountId) {
        return PetProfileResponse.builder()
                .accountId(accountId)
                .mood(50)
                .energy(50)
                .hunger(50)
                .happiness(50)
                .build();
    }
}
