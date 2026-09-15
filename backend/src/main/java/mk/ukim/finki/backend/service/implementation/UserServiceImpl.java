package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.domain.entity.VerificationCode;
import mk.ukim.finki.backend.domain.exception.*;
import mk.ukim.finki.backend.repository.VerificationCodeRepository;
import mk.ukim.finki.backend.service.interfaces.MailService;
import mk.ukim.finki.backend.service.interfaces.UserService;
import mk.ukim.finki.backend.web.request.CreateUserRequest;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.repository.UserRepository;
import mk.ukim.finki.backend.service.interfaces.VerificationCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeService verificationCodeService;
    private final VerificationCodeRepository verificationCodeRepository;
    private final MailService mailService;

    @Value("${verification.code.resend-cooldown-seconds:30}")
    private long resendCooldownSeconds;

    @Override
    public User signUp(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()) {
            throw new EmailAlreadyInUseException(request.email());
        }
        User user = new User(
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );
        userRepository.save(user);
        sendVerificationMail(user);
        return user;
    }

    @Override
    public User logIn(String email, String password) {
        User user = userRepository.findByEmail(email.toLowerCase()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (!user.isEnabled()) {
            if (!verificationCodeService.isValid(user.getId())) {
                sendVerificationMail(user);
            }
            throw new EmailNotVerifiedException(email);
        }
        return user;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase()).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Override
    public void sendVerificationMail(User user) {
        VerificationCode code = verificationCodeService.generateCode(user);
        mailService.sendVerificationCodeEmail(user, code.getCode());
    }

    @Override
    public void verifyEmail(String email, String code) {
        User user = findByEmail(email);
        VerificationCode verificationCode = verificationCodeService.findById(user.getId());

        if (verificationCode.isConsumed()) {
            throw new VerificationCodeAlreadyUsedException(email);
        }
        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new VerificationCodeExpiredException(email);
        }
        if (!verificationCode.getCode().equals(code)) {
            throw new InvalidVerificationCodeException(email);
        }

        verificationCode.setConsumed(true);
        verificationCodeRepository.save(verificationCode);

        user.setEnabled(true);
        userRepository.save(user);
    }

    @Override
    public void resendCode(String email) {
        User user = findByEmail(email);
        verificationCodeService.assertResendAllowed(user.getId(), resendCooldownSeconds);
        sendVerificationMail(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByEmail(username);
    }
}
