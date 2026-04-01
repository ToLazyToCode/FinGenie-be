package fingenie.com.fingenie.service.otp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends OTP emails.
 * Never log raw OTP.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OtpEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@fingenie.com}")
    private String fromEmail;

    @Value("${app.name:FinGenie}")
    private String appName;

    public void sendRegisterOtp(String email, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[" + appName + "] Verify your email");
        msg.setText(String.format(
                "Hello,\n\n" +
                "Your verification code is: %s\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "— %s",
                otp, appName
        ));
        mailSender.send(msg);
        log.info("OTP email sent to {} (purpose=REGISTER)", maskEmail(email));
    }

    public void sendForgotPasswordOtp(String email, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromEmail);
        msg.setTo(email);
        msg.setSubject("[" + appName + "] Reset your password");
        msg.setText(String.format(
                "Hello,\n\n" +
                "Your password reset code is: %s\n\n" +
                "This code expires in 5 minutes.\n\n" +
                "If you did not request this, please ignore this email and ensure your account is secure.\n\n" +
                "— %s",
                otp, appName
        ));
        mailSender.send(msg);
        log.info("OTP email sent to {} (purpose=FORGOT_PASSWORD)", maskEmail(email));
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf("@");
        return email.charAt(0) + "***" + email.substring(at);
    }
}
