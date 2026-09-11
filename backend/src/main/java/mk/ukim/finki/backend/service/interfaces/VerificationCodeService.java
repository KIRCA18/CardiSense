package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.entity.VerificationCode;

import java.util.UUID;

public interface VerificationCodeService {
    VerificationCode findById(UUID uuid);
    VerificationCode generateCode(User user);
    void markConsumed(VerificationCode verificationCode);
    void assertResendAllowed(UUID userId, long cooldownSeconds);

    boolean isValid(UUID userId);
}
