package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ecg_record_ownership")
@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public EcgRecordOwnership(EcgRecord ecgRecord, User user) {
        this.ecgRecord = ecgRecord;
        this.user = user;
    }
}
