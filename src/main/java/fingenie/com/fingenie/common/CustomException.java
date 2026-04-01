package fingenie.com.fingenie.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public CustomException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public CustomException(HttpStatus status, String errorCode) {
        super(errorCode);
        this.status = status;
        this.errorCode = errorCode;
    }

}