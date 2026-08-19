package mk.ukim.finki.backend.domain.exception;

public class InvalidEcgException extends RuntimeException {

    public InvalidEcgException(String message) {
        super(message);
    }

    public InvalidEcgException(String message, Throwable cause) {
        super(message, cause);
    }
}
