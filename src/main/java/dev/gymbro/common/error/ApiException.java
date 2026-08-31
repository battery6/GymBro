package dev.gymbro.common.error;

/**
 * Thrown for expected, client-facing failures. Translated to an RFC 7807
 * response by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final transient ErrorType type;

    public ApiException(ErrorType type) {
        super(type.title());
        this.type = type;
    }

    public ApiException(ErrorType type, String detail) {
        super(detail);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}
