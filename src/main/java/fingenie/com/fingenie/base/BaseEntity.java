package fingenie.com.fingenie.base;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", updatable = false)
    @CreatedDate
    private Timestamp createdAt = Timestamp.valueOf(LocalDateTime.now());

    @Column(name = "updated_at")
    @LastModifiedDate
    private Timestamp updatedAt = Timestamp.valueOf(LocalDateTime.now());

}
