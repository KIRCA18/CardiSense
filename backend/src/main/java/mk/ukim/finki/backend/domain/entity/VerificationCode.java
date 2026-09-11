package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verification_codes")
@Getter
@Setter
public class VerificationCode {
    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean consumed = false;

    protected VerificationCode() {
        // JPA
    }

    public VerificationCode(String code, Instant expiresAt, User user) {
        this.code = code;
        this.expiresAt = expiresAt;
        this.user = user;
    }
}