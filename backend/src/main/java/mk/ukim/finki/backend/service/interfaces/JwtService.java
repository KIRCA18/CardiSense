package mk.ukim.finki.backend.service.interfaces;

import mk.ukim.finki.backend.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String generateToken(User user);
    String extractEmail(String token);
    boolean isTokenValid(String token, UserDetails userDetails);
}
