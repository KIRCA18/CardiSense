package mk.ukim.finki.backend.domain.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class EmailNotVerifiedException extends RuntimeException {
    public EmailNotVerifiedException(String email) {
        super(String.format("Email %s has not been verified", email));
    }
}