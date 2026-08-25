package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ecg_record_ownership")
public class EcgRecordOwnership {

    @Id
    private UUID ecgRecordId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ecg_record_id")
    private EcgRecord ecgRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant linkedAt = Instant.now();

    protected EcgRecordOwnership() {
        // JPA
    }

    public EcgRecordOwnership(EcgRecord ecgRecord, User user) {
        this.ecgRecord = ecgRecord;
        this.user = user;
    }

    public UUID getEcgRecordId() {
        return ecgRecordId;
    }

    public EcgRecord getEcgRecord() {
        return ecgRecord;
    }

    public User getUser() {
        return user;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }
}
