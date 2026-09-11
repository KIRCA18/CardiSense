package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.entity.VerificationCode;
import mk.ukim.finki.backend.domain.exception.ResendTooSoonException;
import mk.ukim.finki.backend.domain.exception.VerificationCodeNotFoundException;
import mk.ukim.finki.backend.repository.VerificationCodeRepository;
import mk.ukim.finki.backend.service.interfaces.VerificationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private static final int CODE_UPPER_BOUND = 1_000_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VerificationCodeRepository verificationCodeRepository;

    @Value("${verification.code.ttl-minutes:10}")
    private long ttlMinutes;

    @Override
    public VerificationCode findById(UUID uuid) {
        return verificationCodeRepository.findById(uuid).orElseThrow(() -> new VerificationCodeNotFoundException(uuid));
    }

    @Override
    public VerificationCode generateCode(User user) {
        String code = String.format("%06d", RANDOM.nextInt(CODE_UPPER_BOUND));
        Instant expiresAt = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);

        VerificationCode verificationCode = verificationCodeRepository
                .findById(user.getId())
                .orElseGet(() -> new VerificationCode(code, expiresAt, user));

        verificationCode.setCode(code);
        verificationCode.setExpiresAt(expiresAt);
        verificationCode.setConsumed(false);

        return verificationCodeRepository.save(verificationCode);
    }

    @Override
    public void markConsumed(VerificationCode verificationCode) {
        verificationCode.setConsumed(true);
        verificationCodeRepository.save(verificationCode);
    }

    @Override
    public void assertResendAllowed(UUID userId, long cooldownSeconds) {
        verificationCodeRepository.findById(userId).ifPresent(vc -> {
            Instant issuedAt = vc.getExpiresAt().minus(ttlMinutes, ChronoUnit.MINUTES);
            if (Instant.now().isBefore(issuedAt.plusSeconds(cooldownSeconds))) {
                throw new ResendTooSoonException(cooldownSeconds);
            }
        });
    }

    @Override
    public boolean isValid(UUID userId) {
        return verificationCodeRepository.findById(userId)
                .map(vc -> !vc.isConsumed() && vc.getExpiresAt().isAfter(Instant.now()))
                .orElse(false);
    }
}
