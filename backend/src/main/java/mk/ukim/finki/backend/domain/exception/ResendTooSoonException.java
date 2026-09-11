package mk.ukim.finki.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class ResendTooSoonException extends RuntimeException {
    public ResendTooSoonException(long cooldownSeconds) {
        super(String.format("Please wait %d seconds before requesting another code", cooldownSeconds));
    }
}