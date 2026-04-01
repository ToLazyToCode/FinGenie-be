package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "friendship")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private Account requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_id", nullable = false)
    private Account addressee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDING;

    public enum FriendshipStatus {
        PENDING("pending"),
        ACCEPTED("accepted"),
        BLOCKED("blocked");

        private final String value;

        FriendshipStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

}
