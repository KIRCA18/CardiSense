package mk.ukim.finki.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class VerificationCodeNotFoundException extends RuntimeException {
    public VerificationCodeNotFoundException(java.util.UUID userId) {
        super(String.format("No verification code found for user %s", userId));
    }
}
