package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.PiggyBankMember;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PiggyBankMemberResponse {
    private Long id;
    private Long piggyId;
    private Long accountId;
    private String accountEmail;
    private PiggyBankMember.MemberRole role;
    private Integer shareWeight;
    private BigDecimal monthlyCommitment;
    private Timestamp joinedAt;
}
