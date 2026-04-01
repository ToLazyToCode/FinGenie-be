package fingenie.com.fingenie.common;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends CustomException {
    public InvalidCredentialsException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_CREDENTIALS",
                "Invalid email or password"
        );
    }
}
