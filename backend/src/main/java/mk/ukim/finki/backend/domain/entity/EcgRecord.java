package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ecg_records")
public class EcgRecord {

    @Id
    @GeneratedValue
    private UUID id;


    @Column(nullable = false)
    private String originalFormat;

    @Column(nullable = false)
    private String storageUri;

    @Column(nullable = false)
    private String waveformUri;

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
    private String checksumSha256;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    protected EcgRecord() {
        // JPA
    }

    public EcgRecord(String originalFormat, String storageUri, String waveformUri,
                      List<String> leadNames, List<String> units, int sampleCount,
                      double samplingRateHz, double durationSeconds, String checksumSha256) {
        this.originalFormat = originalFormat;
        this.storageUri = storageUri;
        this.waveformUri = waveformUri;
        this.leadNames = leadNames;
        this.units = units;
        this.sampleCount = sampleCount;
        this.samplingRateHz = samplingRateHz;
        this.durationSeconds = durationSeconds;
        this.checksumSha256 = checksumSha256;
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalFormat() {
        return originalFormat;
    }

    public String getStorageUri() {
        return storageUri;
    }

    public String getWaveformUri() {
        return waveformUri;
    }

    public List<String> getLeadNames() {
        return leadNames;
    }

    public List<String> getUnits() {
        return units;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public double getSamplingRateHz() {
        return samplingRateHz;
    }

    public double getDurationSeconds() {
        return durationSeconds;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}
