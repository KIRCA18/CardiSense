package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ecg_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EcgRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private EcgUpload upload;

    @Column(nullable = false)
    private String recordName;

    @Column(nullable = false)
    private String originalFormat;


    @ElementCollection
    @CollectionTable(name = "ecg_record_lead_names", joinColumns = @JoinColumn(name = "ecg_record_id"))
    @Column(name = "lead_name")
    @OrderColumn(name = "lead_index")
    private List<String> leadNames;

    @ElementCollection
    @CollectionTable(name = "ecg_record_units", joinColumns = @JoinColumn(name = "ecg_record_id"))
    @Column(name = "unit")
    @OrderColumn(name = "unit_index")
    private List<String> units;

    @Column(nullable = false)
    private int sampleCount;

    @Column(nullable = false)
    private double samplingRateHz;

    @Column(nullable = false)
    private double durationSeconds;



    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    public EcgRecord(
            EcgUpload upload,
            String recordName,
            String originalFormat,
            List<String> leadNames,
            List<String> units,
            int sampleCount,
            double samplingRateHz,
            double durationSeconds
    ) {
        this.upload = upload;
        this.recordName = recordName;
        this.originalFormat = originalFormat;
        this.leadNames = List.copyOf(leadNames);
        this.units = List.copyOf(units);
        this.sampleCount = sampleCount;
        this.samplingRateHz = samplingRateHz;
        this.durationSeconds = durationSeconds;
    }
}
