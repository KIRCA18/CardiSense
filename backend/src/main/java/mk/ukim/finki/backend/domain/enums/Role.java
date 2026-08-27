package mk.ukim.finki.backend.domain.enums;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ROLE_RESEARCHER;

    @Override
    public String getAuthority() {
        return name();
    }
}
