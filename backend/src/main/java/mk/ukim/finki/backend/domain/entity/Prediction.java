package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private double score;

    @Column(nullable = false)
    private String aggregation;

    protected Prediction() {
        // JPA
    }

    public Prediction(Analysis analysis, String label, double score, String aggregation) {
        this.analysis = analysis;
        this.label = label;
        this.score = score;
        this.aggregation = aggregation;
    }

    public UUID getId() {
        return id;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public String getLabel() {
        return label;
    }

    public double getScore() {
        return score;
    }

    public String getAggregation() {
        return aggregation;
    }
}
