package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prompt_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromptTemplate extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // CHAT, PREDICTION, INSIGHT, GAMIFICATION

    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    private String templateText;

    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 1L;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isActive = true;
}
