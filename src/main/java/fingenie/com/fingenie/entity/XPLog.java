package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "xp_log")
@Getter
@Setter
public class XPLog extends BaseEntity {

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "xp_amount", nullable = false)
    private Integer xpAmount;

    @Column(name = "description", length = 255)
    private String description;
}
