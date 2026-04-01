package fingenie.com.fingenie.ai.client;

import lombok.Getter;

@Getter
public class AIClientException extends RuntimeException {

    public enum FailureType {
        TIMEOUT,
        CONNECT_ERROR,
        HTTP_ERROR,
        PARSE_ERROR,
        EMPTY_RESPONSE,
        UNKNOWN_ERROR
    }

    private final String path;
    private final FailureType failureType;
    private final long elapsedMs;
    private final long timeoutMs;
    private final Integer statusCode;
    private final String providerMessage;

    public AIClientException(
            String path,
            FailureType failureType,
            String message,
            long elapsedMs,
            long timeoutMs,
            Integer statusCode,
            String providerMessage,
            Throwable cause
    ) {
        super(message, cause);
        this.path = path;
        this.failureType = failureType;
        this.elapsedMs = elapsedMs;
        this.timeoutMs = timeoutMs;
        this.statusCode = statusCode;
        this.providerMessage = providerMessage;
    }

    public AIClientException withElapsedMs(long nextElapsedMs) {
        return new AIClientException(
                path,
                failureType,
                getMessage(),
                nextElapsedMs,
                timeoutMs,
                statusCode,
                providerMessage,
                getCause()
        );
    }
}
