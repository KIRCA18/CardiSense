package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "analysis_warnings")
public class AnalysisWarning {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false)
    private String message;

    protected AnalysisWarning() {
        // JPA
    }

    public AnalysisWarning(Analysis analysis, String message) {
        this.analysis = analysis;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public Analysis getAnalysis() {
        return analysis;
    }

    public String getMessage() {
        return message;
    }
}
