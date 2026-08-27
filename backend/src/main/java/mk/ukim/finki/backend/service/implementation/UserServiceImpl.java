package mk.ukim.finki.backend.service.implementation;

import lombok.RequiredArgsConstructor;
import mk.ukim.finki.backend.service.interfaces.UserService;
import mk.ukim.finki.backend.domain.dto.CreateUserRequest;
import mk.ukim.finki.backend.domain.entity.User;
import mk.ukim.finki.backend.domain.exception.EmailAlreadyInUseException;
import mk.ukim.finki.backend.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public User signUp(CreateUserRequest request) {
        if (userRepository.findByEmail(request.email().toLowerCase()).isPresent()){
            throw new EmailAlreadyInUseException(request.email());
        }
        User user = new User(
                request.email().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName()
        );
        userRepository.save(user);
        return user;
    }

    @Override
    public User logIn(String email, String password) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("Invalid username or password")
        );
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        return user;
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
    }

    @Override
    public void sendVerificationMail(User user) {
        //TODO: to be defined
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return findByEmail(username);
    }
}
