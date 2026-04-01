package fingenie.com.fingenie.ai.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ai_embedding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIEmbedding extends BaseEntity {

    private Long userId;

    @Lob
    @Column(nullable = false)
    private byte[] vector;

    private String sourceType;

    private Long referenceId;
}