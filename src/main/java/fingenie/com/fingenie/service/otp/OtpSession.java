package fingenie.com.fingenie.service.otp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * OTP session stored in Redis.
 * Key: otp:session:{sessionId}
 * TTL: 5 minutes
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OtpSession implements Serializable {
    private String email;
    private String otpHash;      // SHA-256 of OTP - never store raw OTP
    private int attemptCount;
    private Instant expireAt;
    private OtpPurpose purpose;

    // Register flow - store pending account data
    private String passwordHash; // bcrypt
    private String fullName;
    private String dateOfBirth;
}
