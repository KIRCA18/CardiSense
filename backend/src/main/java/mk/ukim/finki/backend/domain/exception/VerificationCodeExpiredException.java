package mk.ukim.finki.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException(String email) {
        super(String.format("Verification code for %s has expired", email));
    }
}