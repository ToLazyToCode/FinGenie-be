package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pet")
@Getter
@Setter
public class Pet extends BaseEntity {

    private Long accountId;

    private String petName;
    private String petType;
    private String petAvatarUrl;

    private Integer level;
    private Integer experiencePoints;

    private String mood;
    private String personality;
}
