package fingenie.com.fingenie.common;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends CustomException {

    public EmailAlreadyExistsException(String email) {
        super(
                HttpStatus.BAD_REQUEST,
                "AUTH_EMAIL_EXISTS",
                "Email already registered: " + email
        );
    }
}
