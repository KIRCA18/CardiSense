package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "predictions")
@Data
@AllArgsConstructor
@NoArgsConstructor
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

    public Prediction(Analysis analysis, String label, double score, String aggregation) {
        this.analysis = analysis;
        this.label = label;
        this.score = score;
        this.aggregation = aggregation;
    }

}
