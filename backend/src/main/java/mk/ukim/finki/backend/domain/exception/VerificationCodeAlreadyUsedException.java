package mk.ukim.finki.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class VerificationCodeAlreadyUsedException extends RuntimeException {
    public VerificationCodeAlreadyUsedException(String email) {
        super(String.format("Verification code for %s was already used", email));
    }
}