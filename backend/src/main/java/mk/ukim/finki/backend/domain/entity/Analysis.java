package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import mk.ukim.finki.backend.domain.model.AnalysisResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "analyses")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ecg_record_id", nullable = false)
    private EcgRecord ecgRecord;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private MlModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id")
    private User requestedBy;

    @Column(nullable = false)
    private String deviceUsed;

    @Column(nullable = false)
    private String checkpointUsed;

    @Column(nullable = false)
    private int inputLeadCount;

    @Column(nullable = false)
    private int inputSampleCount;

    @Column(nullable = false)
    private double inputSamplingRateHz;

    @Column(nullable = false)
    private double inputDurationSeconds;

    @Column(nullable = false)
    private int resampledSampleCount;

    @Column(nullable = false)
    private int segmentsProcessed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    private Instant startedAt;

    private Instant completedAt;

    private String errorMessage;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions = new ArrayList<>();

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnalysisWarning> warnings = new ArrayList<>();

    public enum Status {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    public Analysis(EcgRecord ecgRecord, MlModel model, User requestedBy, String deviceUsed,
                     String checkpointUsed, int inputLeadCount, int inputSampleCount,
                     double inputSamplingRateHz, double inputDurationSeconds,
                     int resampledSampleCount, int segmentsProcessed) {
        this.ecgRecord = ecgRecord;
        this.model = model;
        this.requestedBy = requestedBy;
        this.deviceUsed = deviceUsed;
        this.checkpointUsed = checkpointUsed;
        this.inputLeadCount = inputLeadCount;
        this.inputSampleCount = inputSampleCount;
        this.inputSamplingRateHz = inputSamplingRateHz;
        this.inputDurationSeconds = inputDurationSeconds;
        this.resampledSampleCount = resampledSampleCount;
        this.segmentsProcessed = segmentsProcessed;
    }

    public static Analysis completed(
            EcgRecord ecgRecord,
            MlModel model,
            User requestedBy,
            AnalysisResult result,
            Instant startedAt,
            Instant completedAt
    ) {
        Analysis analysis = new Analysis(
                ecgRecord,
                model,
                requestedBy,
                result.model().device(),
                result.model().checkpoint(),
                result.input().leadCount(),
                result.input().sampleCount(),
                result.input().samplingRateHz(),
                result.input().durationSeconds(),
                result.input().resampledSampleCount(),
                result.input().segmentsProcessed()
        );

        analysis.status = Status.COMPLETED;
        analysis.startedAt = startedAt;
        analysis.completedAt = completedAt;

        result.predictions().forEach(prediction ->
                analysis.predictions.add(
                        new Prediction(
                                analysis,
                                prediction.label(),
                                prediction.score(),
                                prediction.aggregation()
                        )
                )
        );

        result.warnings().forEach(warning ->
                analysis.warnings.add(
                        new AnalysisWarning(analysis, warning)
                )
        );

        return analysis;
    }
}
