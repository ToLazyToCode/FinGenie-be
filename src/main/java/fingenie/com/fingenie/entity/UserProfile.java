package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Date;

@Entity
@Table(name = "user_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @Column(name = "phone_number", nullable = true)
    private String phoneNumber;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "date_of_birth")
    private Date dateOfBirth;
    
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
