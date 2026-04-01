package fingenie.com.fingenie.common;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends CustomException {
    public InvalidRefreshTokenException() {
        super(
                HttpStatus.UNAUTHORIZED,
                "AUTH_INVALID_REFRESH_TOKEN",
                "Refresh token is invalid or expired"
        );
    }
}
