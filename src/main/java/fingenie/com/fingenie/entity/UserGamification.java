package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_gamification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGamification extends BaseEntity {

    private Long accountId;

    @Builder.Default
    private Integer xp = 0;

    @Builder.Default
    private Integer level = 1;

}
