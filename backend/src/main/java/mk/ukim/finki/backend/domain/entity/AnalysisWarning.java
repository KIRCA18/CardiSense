package mk.ukim.finki.backend.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "analysis_warnings")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalysisWarning {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(nullable = false)
    private String message;

    public AnalysisWarning(Analysis analysis, String message) {
        this.analysis = analysis;
        this.message = message;
    }
}
