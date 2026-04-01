package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pet_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetProfile extends BaseEntity {

    private Long accountId;

    @Builder.Default
    private Integer mood = 50; // 0-100

    @Builder.Default
    private Integer energy = 50;

    @Builder.Default
    private Integer hunger = 50;

    @Builder.Default
    private Integer happiness = 50;

}
