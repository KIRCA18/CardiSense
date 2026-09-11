package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.web.request.CreateUserRequest;
import mk.ukim.finki.backend.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {
    User signUp(CreateUserRequest request);
    User logIn(String email, String password);
    User findByEmail(String email);

    void sendVerificationMail(User user);
    void verifyEmail(String email, String code);
    void resendCode(String email);
}
